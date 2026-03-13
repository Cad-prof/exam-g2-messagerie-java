package com.g2.dto;

import com.g2.model.Message;
import com.g2.model.User;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Objet échangé entre client et serveur sur le réseau.
 *
 * Serializable : permet la conversion en bytes pour ObjectOutputStream.
 * serialVersionUID : version de sérialisation — si la classe change,
 * changer ce numéro évite des erreurs de désérialisation avec
 * d'anciens fichiers/connexions.
 *
 * On utilise des factory methods statiques plutôt qu'un constructeur public :
 * plus clair à lire (Packet.login(...) vs new Packet(LOGIN, ...)).
 */
public class Packet implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        // Depuis le client
        REGISTER, LOGIN, LOGOUT, SEND_MSG, GET_HISTORY,
        // Depuis le serveur
        LOGIN_SUCCESS, SUCCESS, ERROR,
        INCOMING_MSG, HISTORY_RESPONSE,
        USER_CONNECTED, USER_DISCONNECTED
    }

    private Type          type;
    private String        username;
    private String        password;
    private User.Role     role;
    private String        receiverUsername;
    private String        contenu;
    private String        statusMessage; // message de succès ou d'erreur
    private User          user;          // objet User complet (à la connexion)
    private Message       message;       // objet Message complet
    private LocalDateTime timestamp;

    // Constructeur privé → on passe par les factory methods
    private Packet(Type type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    // ── Factory methods ──────────────────────────────────────────────

    public static Packet register(String username, String password, User.Role role) {
        Packet p = new Packet(Type.REGISTER);
        p.username = username;
        p.password = password;
        p.role = role;
        return p;
    }

    public static Packet login(String username, String password) {
        Packet p = new Packet(Type.LOGIN);
        p.username = username;
        p.password = password;
        return p;
    }

    public static Packet logout() {
        return new Packet(Type.LOGOUT);
    }

    public static Packet sendMessage(String receiverUsername, String contenu) {
        Packet p = new Packet(Type.SEND_MSG);
        p.receiverUsername = receiverUsername;
        p.contenu = contenu;
        return p;
    }

    public static Packet loginSuccess(User user) {
        Packet p = new Packet(Type.LOGIN_SUCCESS);
        p.user = user;
        return p;
    }

    public static Packet incomingMessage(Message message) {
        Packet p = new Packet(Type.INCOMING_MSG);
        p.message = message;
        return p;
    }

    public static Packet success(String message) {
        Packet p = new Packet(Type.SUCCESS);
        p.statusMessage = message;
        return p;
    }

    public static Packet error(String message) {
        Packet p = new Packet(Type.ERROR);
        p.statusMessage = message;
        return p;
    }

    public static Packet userConnected(String username) {
        Packet p = new Packet(Type.USER_CONNECTED);
        p.username = username;
        return p;
    }

    public static Packet userDisconnected(String username) {
        Packet p = new Packet(Type.USER_DISCONNECTED);
        p.username = username;
        return p;
    }

    // ── Getters ──────────────────────────────────────────────────────

    public Type          getType()            { return type; }
    public String        getUsername()        { return username; }
    public String        getPassword()        { return password; }
    public User.Role     getRole()            { return role; }
    public String        getReceiverUsername(){ return receiverUsername; }
    public String        getContenu()         { return contenu; }
    public String        getStatusMessage()   { return statusMessage; }
    public User          getUser()            { return user; }
    public Message       getMessage()         { return message; }
    public LocalDateTime getTimestamp()       { return timestamp; }
}