package com.g2.server;

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

public class ClientHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final ConcurrentHashMap<String, ClientHandler> connectedClients;

    private BufferedReader in;
    private PrintWriter    out;
    private User           currentUser;

    private final UserDAO    userDAO    = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> connectedClients) {
        this.socket           = socket;
        this.connectedClients = connectedClients;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            String line;
            while ((line = in.readLine()) != null) handleCommand(line.trim());
        } catch (IOException e) {
            logger.warning("[RG10] Perte connexion : "
                    + (currentUser != null ? currentUser.getUsername() : "inconnu"));
        } finally {
            disconnect();
        }
    }

    private void handleCommand(String line) {
        if (line.isEmpty()) return;
        String[] parts   = line.split("\\|", -1);
        String   command = parts[0].toUpperCase();
        logger.info("← COMMANDE : " + line);

        switch (command) {
            case "REGISTER" -> handleRegister(parts);
            case "LOGIN"    -> handleLogin(parts);
            case "LOGOUT"   -> handleLogout();
            case "SEND"     -> handleSend(parts);
            case "HISTORY"  -> handleHistory(parts);
            case "MEMBERS"  -> handleMembers();
            case "LIST"     -> handleListUsers();
            default         -> send("ERROR|Commande inconnue : " + command);
        }
    }

    // REGISTER — RG1, RG9
    private void handleRegister(String[] parts) {
        if (parts.length < 4) { send("ERROR|Usage : REGISTER|username|password|role"); return; }

        String username = parts[1].trim();
        String password = parts[2];
        String roleStr  = parts[3].toUpperCase().trim();

        if (username.isEmpty()) { send("ERROR|Username vide."); return; }
        if (password.isEmpty()) { send("ERROR|Mot de passe vide."); return; }

        User.Role role;
        try { role = User.Role.valueOf(roleStr); }
        catch (IllegalArgumentException e) {
            send("ERROR|Rôle invalide. Valeurs : ORGANISATEUR, MEMBRE, BENEVOLE"); return;
        }

        if (userDAO.findByUsername(username) != null) {
            send("ERROR|Ce nom d'utilisateur est déjà pris. (RG1)"); return;
        }

        userDAO.save(new User(username, PasswordUtil.hash(password), role));
        logger.info("[RG12] Inscription : " + username + " | Rôle : " + role);
        send("OK|Inscription réussie. Vous pouvez vous connecter.");
    }

    // LOGIN — RG2, RG3, RG4
    private void handleLogin(String[] parts) {
        if (parts.length < 3) { send("ERROR|Usage : LOGIN|username|password"); return; }

        String username = parts[1].trim();
        String password = parts[2];

        User user = userDAO.findByUsername(username);
        if (user == null || !PasswordUtil.verify(password, user.getPassword())) {
            send("ERROR|Identifiants incorrects. (RG2)"); return;
        }

        if (connectedClients.containsKey(username)) {
            send("ERROR|Déjà connecté sur un autre appareil. (RG3)"); return;
        }

        currentUser = user;
        currentUser.setStatus(User.Status.ONLINE);
        userDAO.update(currentUser);
        connectedClients.put(username, this);

        logger.info("[RG12] Connexion : " + username + " (" + user.getRole() + ") → ONLINE");

        // FORMAT CORRECT : OK|LOGIN|ROLE  (lu par LoginController)
        send("OK|LOGIN|" + user.getRole());

        // RG6 : messages en attente
        deliverPendingMessages();

        // Contacts selon le rôle
        handleMembers();
    }

    // LOGOUT — RG4
    private void handleLogout() {
        if (currentUser == null) { send("ERROR|Non connecté."); return; }
        send("OK|Déconnexion réussie.");
        disconnect();
    }

    // SEND — RG5, RG6, RG7
    private void handleSend(String[] parts) {
        if (!checkAuth()) return;
        if (currentUser.getRole() == User.Role.BENEVOLE) {
            send("ERROR|Les BENEVOLES ne peuvent pas envoyer de messages."); return;
        }
        if (parts.length < 3) { send("ERROR|Usage : SEND|destinataire|contenu"); return; }

        String receiverUsername = parts[1].trim();
        String contenu          = parts[2];

        if (contenu == null || contenu.isBlank()) { send("ERROR|Message vide. (RG7)"); return; }
        if (contenu.length() > 1000) { send("ERROR|Message > 1000 caractères. (RG7)"); return; }

        User receiver = userDAO.findByUsername(receiverUsername);
        if (receiver == null) { send("ERROR|Destinataire introuvable. (RG5)"); return; }

        if (currentUser.getRole() == User.Role.MEMBRE
                && receiver.getRole() == User.Role.BENEVOLE) {
            send("ERROR|Vous ne pouvez pas envoyer un message à un BENEVOLE."); return;
        }

        Message message = new Message(currentUser, receiver, contenu);
        messageDAO.save(message);
        logger.info("[RG12] Message : " + currentUser.getUsername() + " → " + receiverUsername);
        send("OK|Message envoyé.");

        ClientHandler receiverHandler = connectedClients.get(receiverUsername);
        if (receiverHandler != null) {
            receiverHandler.send("MESSAGE|" + currentUser.getUsername() + "|" + contenu);
            message.setStatut(Message.Statut.RECU);
            messageDAO.update(message);
        } else {
            logger.info("[RG6] " + receiverUsername + " OFFLINE → message stocké");
        }
    }

    // HISTORY — RG8
    private void handleHistory(String[] parts) {
        if (!checkAuth()) return;
        if (parts.length < 2) { send("ERROR|Usage : HISTORY|username"); return; }

        User other = userDAO.findByUsername(parts[1].trim());
        if (other == null) { send("ERROR|Utilisateur introuvable."); return; }

        List<Message> history = messageDAO.findConversation(currentUser, other);
        send("HISTORY_START|" + history.size());
        for (Message m : history) {
            send("MSG|" + m.getSender().getUsername()
                    + "|" + m.getContenu()
                    + "|" + m.getDateEnvoi());
        }
        send("HISTORY_END");
    }

    // MEMBERS — contacts filtrés selon le rôle
    private void handleMembers() {
        if (!checkAuth()) return;

        List<User> users = userDAO.findAll();
        send("MEMBERS_START");
        for (User u : users) {
            if (u.getUsername().equals(currentUser.getUsername())) continue;
            boolean canSee = switch (currentUser.getRole()) {
                case ORGANISATEUR -> true;
                case MEMBRE       -> u.getRole() != User.Role.BENEVOLE;
                case BENEVOLE     -> false;
            };
            if (canSee) {
                String status = u.getStatus() == User.Status.ONLINE ? "ONLINE" : "OFFLINE";
                send("MEMBER|" + u.getUsername() + "|" + status + "|" + u.getRole());
            }
        }
        send("MEMBERS_END");
    }

    // LIST — RG13 ORGANISATEUR uniquement
    private void handleListUsers() {
        if (!checkAuth()) return;
        if (currentUser.getRole() != User.Role.ORGANISATEUR) {
            send("ERROR|Accès refusé. Réservé aux ORGANISATEURS. (RG13)"); return;
        }

        List<User> users = userDAO.findAll();
        logger.info("[RG13] Liste consultée par : " + currentUser.getUsername());
        send("LIST_START|" + users.size());
        for (User u : users) {
            String status = u.getStatus() == User.Status.ONLINE ? "ONLINE" : "OFFLINE";
            send("USER|" + u.getUsername() + "|" + u.getRole() + "|" + status);
        }
        send("LIST_END");
    }

    // RG6 : messages en attente
    private void deliverPendingMessages() {
        List<Message> pending = messageDAO.findPendingMessages(currentUser);
        if (pending.isEmpty()) return;
        logger.info("[RG6] " + pending.size() + " message(s) en attente pour " + currentUser.getUsername());
        for (Message m : pending) {
            send("MESSAGE|" + m.getSender().getUsername() + "|" + m.getContenu());
            m.setStatut(Message.Statut.RECU);
            messageDAO.update(m);
        }
    }

    private boolean checkAuth() {
        if (currentUser == null) {
            send("ERROR|Vous devez être connecté. (RG2)");
            return false;
        }
        return true;
    }

    // RG4 + RG10 + RG12
    private void disconnect() {
        if (currentUser != null) {
            currentUser.setStatus(User.Status.OFFLINE);
            userDAO.update(currentUser);
            connectedClients.remove(currentUser.getUsername());
            logger.info("[RG12] Déconnexion : " + currentUser.getUsername() + " → OFFLINE");
            currentUser = null;
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    public void send(String message) {
        if (out != null) {
            out.println(message);
            logger.info("→ ENVOI : " + message);
        }
    }
}