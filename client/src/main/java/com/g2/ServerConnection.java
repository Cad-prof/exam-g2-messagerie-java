package com.g2;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Gère la connexion Socket vers le serveur (singleton).
 * RG10 : en cas de perte de connexion, notifie le client et passe OFFLINE.
 */
public class ServerConnection {

    private static final Logger logger = Logger.getLogger(ServerConnection.class.getName());
    private static final String HOST = "localhost";
    private static final int    PORT = 5000;

    private static ServerConnection instance;

    private Socket           socket;
    private PrintWriter      out;
    private BufferedReader   in;
    private Consumer<String> messageListener;
    private boolean          connected = false;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) instance = new ServerConnection();
        return instance;
    }

    // Connexion au serveur
    public boolean connect() {
        try {
            socket    = new Socket(HOST, PORT);
            out       = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in        = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            startListening();
            logger.info("Connecté au serveur " + HOST + ":" + PORT);
            return true;
        } catch (IOException e) {
            logger.severe("Impossible de se connecter au serveur : " + e.getMessage());
            return false;
        }
    }

    // Écoute des messages entrants (thread séparé)

    private void startListening() {
        Thread listener = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (messageListener != null) {
                        final String msg = line;
                        javafx.application.Platform.runLater(() -> messageListener.accept(msg));
                    }
                }
            } catch (IOException e) {
                logger.warning("[RG10] Connexion perdue : " + e.getMessage());
            } finally {
                // RG10 : perte de connexion → notifier l'interface et passer OFFLINE
                connected = false;
                if (messageListener != null) {
                    javafx.application.Platform.runLater(() ->
                            messageListener.accept("OFFLINE|Connexion au serveur perdue. Vous êtes maintenant hors ligne.")
                    );
                }
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    // Envoi d'une commande au serveur

    public void send(String command) {
        if (out != null && connected) {
            out.println(command);
        }
    }

    // Déconnexion propre

    public void disconnect() {
        if (connected) send("LOGOUT");
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        connected = false;
    }

    // Getters / Setters

    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    public boolean isConnected() { return connected; }
}