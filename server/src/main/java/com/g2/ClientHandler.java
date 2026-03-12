package com.g2;

import com.g2.dao.MessageDAO;
import com.g2.dao.UserDAO;
import com.g2.model.Message;
import com.g2.model.User;
import com.g2.util.PasswordUtil;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Gère la communication avec UN client dans son propre thread (RG11).
 * Protocole : COMMANDE|param1|param2|...
 *
 * RG1  : username unique (inscription)
 * RG2  : authentification requise pour SEND, HISTORY, MEMBERS, LIST
 * RG3  : un seul login par utilisateur à la fois
 * RG4  : ONLINE à connexion, OFFLINE à déconnexion/perte réseau
 * RG5  : expéditeur ONLINE + destinataire existant
 * RG6  : message stocké si OFFLINE, livré à la reconnexion
 * RG7  : message non vide, max 1000 caractères
 * RG8  : historique chronologique (ORDER BY dans DAO)
 * RG9  : mot de passe haché BCrypt
 * RG10 : perte réseau → déconnexion propre
 * RG11 : thread séparé par client
 * RG12 : journalisation complète
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final ConcurrentHashMap<String, ClientHandler> connectedClients;

    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    private final UserDAO    userDAO    = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> connectedClients) {
        this.socket = socket;
        this.connectedClients = connectedClients;
    }

    // Boucle principale  : thread séparé par client

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleCommand(line.trim());
            }

        } catch (IOException e) {
            // RG10 : perte réseau détectée côté serveur
            logger.warning("[RG10] Perte de connexion pour "
                    + (currentUser != null ? currentUser.getUsername() : "inconnu")
                    + " : " + e.getMessage());
        } finally {
            // RG4 + RG10 : toujours passer OFFLINE en quittant
            disconnect();
        }
    }

    // =========================================================
    // Routeur de commandes
    // =========================================================

    private void handleCommand(String line) {
        if (line.isEmpty()) return;
        String[] parts   = line.split("\\|", -1);
        String   command = parts[0].toUpperCase();

        switch (command) {
            case "REGISTER" -> handleRegister(parts);
            case "LOGIN"    -> handleLogin(parts);
            case "LOGOUT"   -> handleLogout();
            case "SEND"     -> handleSend(parts);
            case "HISTORY"  -> handleHistory(parts);
            case "MEMBERS"  -> handleMembers();
            case "LIST"     -> handleListUsers(); // RG13
            default         -> send("ERROR|Commande inconnue : " + command);
        }
    }

    // =========================================================
    // REGISTER|username|password|role
    // RG1 : username unique | RG9 : mot de passe haché
    // =========================================================

    private void handleRegister(String[] parts) {
        if (parts.length < 4) { send("ERROR Usage : REGISTER|username|password|role"); return; }

        String username = parts[1].trim();
        String password = parts[2];
        String roleStr  = parts[3].toUpperCase().trim();

        if (username.isEmpty()) { send("ERROR|Le nom d'utilisateur ne peut pas être vide."); return; }
        if (password.isEmpty()) { send("ERROR|Le mot de passe ne peut pas être vide."); return; }

        // Validation rôle
        User.Role role;
        try {
            role = User.Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            send("ERROR|Rôle invalide. Valeurs : ORGANISATEUR, MEMBRE, BENEVOLE");
            return;
        }

        // RG1 : username unique
        if (userDAO.findByUsername(username) != null) {
            send("ERROR|Ce nom d'utilisateur est déjà pris.");
            return;
        }

        // RG9 : hachage BCrypt
        String hashedPassword = PasswordUtil.hash(password);
        userDAO.save(new User(username, hashedPassword, role));

        logger.info("Inscription : " + username + " | Rôle : " + role);
        send("OK Inscription réussie. Vous pouvez vous connecter.");
    }

    // =========================================================
    // LOGIN|username|password
    // RG2 : credentials | RG3 : unicité session | RG4 : ONLINE
    // =========================================================

    private void handleLogin(String[] parts) {
        if (parts.length < 3) { send("ERROR|Usage : LOGIN|username|password"); return; }

        String username = parts[1].trim();
        String password = parts[2];

        // RG2 : vérification credentials
        User user = userDAO.findByUsername(username);
        if (user == null || !PasswordUtil.verify(password, user.getPassword())) {
            send("ERROR|Identifiants incorrects. (RG2)");
            return;
        }

        // RG3 : un seul login à la fois
        if (connectedClients.containsKey(username)) {
            send("ERROR|Cet utilisateur est déjà connecté sur un autre appareil. (RG3)");
            return;
        }

        // RG4 : statut → ONLINE
        currentUser = user;
        currentUser.setStatus(User.Status.ONLINE);
        userDAO.update(currentUser);
        connectedClients.put(username, this);

        logger.info("[RG12] Connexion : " + username + " → ONLINE");
        send("OK|Bienvenue " + username + "!");

        // RG6 : livraison messages en attente
        deliverPendingMessages();

        // Envoyer la liste des membres automatiquement
        handleMembers();
    }

    // =========================================================
    // LOGOUT — RG4 : OFFLINE
    // =========================================================

    private void handleLogout() {
        if (currentUser == null) { send("ERROR|Vous n'êtes pas connecté."); return; }
        send("OK|Déconnexion réussie.");
        disconnect();
    }

    // =========================================================
    // SEND|receiverUsername|contenu
    // RG2 : authentifié | RG5 : expéditeur ONLINE + destinataire existant
    // RG6 : stockage si OFFLINE | RG7 : contenu valide
    // =========================================================

    private void handleSend(String[] parts) {
        // RG2
        if (currentUser == null) {
            send("ERROR|Vous devez être connecté pour envoyer un message. (RG2)");
            return;
        }
        if (parts.length < 3) { send("ERROR|Usage : SEND|destinataire|contenu"); return; }

        String receiverUsername = parts[1].trim();
        String contenu          = parts[2];

        // RG7 : contenu non vide
        if (contenu == null || contenu.isBlank()) {
            send("ERROR|Le message ne peut pas être vide. (RG7)");
            return;
        }
        // RG7 : max 1000 caractères
        if (contenu.length() > 1000) {
            send("ERROR|Le message dépasse 1000 caractères (" + contenu.length() + "/1000). (RG7)");
            return;
        }

        // RG5 : destinataire doit exister
        User receiver = userDAO.findByUsername(receiverUsername);
        if (receiver == null) {
            send("ERROR|Destinataire introuvable : " + receiverUsername + " (RG5)");
            return;
        }

        // RG5 : expéditeur doit être ONLINE (vérification en base)
        User senderCheck = userDAO.findByUsername(currentUser.getUsername());
        if (senderCheck == null || senderCheck.getStatus() != User.Status.ONLINE) {
            send("ERROR|Votre session a expiré. Reconnectez-vous. (RG5)");
            return;
        }

        // Sauvegarde du message
        Message message = new Message(currentUser, receiver, contenu);
        messageDAO.save(message);

        logger.info("[RG12] Message : " + currentUser.getUsername()
                + " → " + receiverUsername + " | " + contenu.length() + " car.");
        send("OK|Message envoyé.");

        // RG6 : livraison immédiate si ONLINE
        ClientHandler receiverHandler = connectedClients.get(receiverUsername);
        if (receiverHandler != null) {
            receiverHandler.send("MESSAGE|" + currentUser.getUsername() + "|" + contenu);
            message.setStatut(Message.Statut.RECU);
            messageDAO.update(message);
            logger.info("[RG6] Livré immédiatement à " + receiverUsername + " (ONLINE)");
        } else {
            logger.info("[RG6] " + receiverUsername + " OFFLINE → message en attente");
        }
    }

    // =========================================================
    // HISTORY|otherUsername
    // RG2 : authentifié | RG8 : ordre chronologique
    // =========================================================

    private void handleHistory(String[] parts) {
        // RG2
        if (currentUser == null) {
            send("ERROR|Vous devez être connecté pour consulter les messages. (RG2)");
            return;
        }
        if (parts.length < 2) { send("ERROR|Usage : HISTORY|username"); return; }

        String otherUsername = parts[1].trim();
        User other = userDAO.findByUsername(otherUsername);
        if (other == null) { send("ERROR|Utilisateur introuvable : " + otherUsername); return; }

        // RG8 : ORDER BY dateEnvoi ASC (géré dans MessageDAO.findConversation)
        List<Message> history = messageDAO.findConversation(currentUser, other);

        logger.info("[RG12] Historique : " + currentUser.getUsername()
                + " ↔ " + otherUsername + " | " + history.size() + " message(s)");

        send("HISTORY_START|" + history.size());
        for (Message m : history) {
            send("MSG|" + m.getSender().getUsername()
                    + "|" + m.getContenu()
                    + "|" + m.getDateEnvoi());
        }
        send("HISTORY_END");
    }

    // =========================================================
    // MEMBERS — tous les membres sauf soi-même (RG2)
    // =========================================================

    private void handleMembers() {
        if (currentUser == null) { send("ERROR|Vous devez être connecté. (RG2)"); return; }

        List<User> users = userDAO.findAll();
        send("MEMBERS_START|" + users.size());
        for (User u : users) {
            if (!u.getUsername().equals(currentUser.getUsername())) {
                send("MEMBER|" + u.getUsername() + "|" + u.getStatus());
            }
        }
        send("MEMBERS_END");
    }

    // =========================================================
    // LIST — liste avec rôles (RG13 : ORGANISATEUR uniquement)
    // =========================================================

    private void handleListUsers() {
        if (currentUser == null) { send("ERROR|Vous devez être connecté. (RG2)"); return; }
        if (currentUser.getRole() != User.Role.ORGANISATEUR) {
            send("ERROR|Accès refusé. Réservé aux ORGANISATEURS. (RG13)");
            return;
        }

        List<User> users = userDAO.findAll();
        logger.info("[RG12] Liste complète consultée par : " + currentUser.getUsername());

        send("LIST_START|" + users.size());
        for (User u : users) {
            send("USER|" + u.getUsername() + "|" + u.getRole() + "|" + u.getStatus());
        }
        send("LIST_END");
    }

    // =========================================================
    // Livraison messages en attente — RG6
    // =========================================================

    private void deliverPendingMessages() {
        List<Message> pending = messageDAO.findPendingMessages(currentUser);
        if (pending.isEmpty()) return;

        logger.info("[RG6] Livraison de " + pending.size()
                + " message(s) en attente pour " + currentUser.getUsername());

        send("PENDING|" + pending.size() + " message(s) en attente.");
        for (Message m : pending) {
            send("MESSAGE|" + m.getSender().getUsername() + "|" + m.getContenu());
            m.setStatut(Message.Statut.RECU);
            messageDAO.update(m);
        }
    }

    // =========================================================
    // Déconnexion propre — RG4 + RG10 + RG12
    // =========================================================

    private void disconnect() {
        if (currentUser != null) {
            // RG4 : statut → OFFLINE
            currentUser.setStatus(User.Status.OFFLINE);
            userDAO.update(currentUser);
            connectedClients.remove(currentUser.getUsername());

            // RG12 : journalisation
            logger.info("[RG12] Déconnexion : " + currentUser.getUsername() + " → OFFLINE");
            currentUser = null;
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    // =========================================================
    // Envoi au client
    // =========================================================

    public void send(String message) {
        if (out != null) out.println(message);
    }
}