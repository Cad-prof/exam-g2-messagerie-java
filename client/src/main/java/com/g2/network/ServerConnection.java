package com.g2.network;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ServerConnection {

    private static final Logger logger   = Logger.getLogger(ServerConnection.class.getName());
    private static final String HOST     = "localhost";
    private static final int    PORT     = 5000;

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

    // Connexion — ne reconnecte que si pas déjà connecté
    public boolean connect() {
        if (connected && socket != null && !socket.isClosed()) {
            return true; // déjà connecté, rien à faire
        }
        try {
            socket    = new Socket(HOST, PORT);
            out       = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in        = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            startListening();
            logger.info("Connecté au serveur " + HOST + ":" + PORT);
            return true;
        } catch (IOException e) {
            connected = false;
            logger.severe("Impossible de se connecter : " + e.getMessage());
            return false;
        }
    }

    private void startListening() {
        Thread listener = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (messageListener != null) {
                        final String msg = line;
                        javafx.application.Platform.runLater(
                                () -> messageListener.accept(msg));
                    }
                }
            } catch (IOException e) {
                logger.warning("[RG10] Connexion perdue : " + e.getMessage());
            } finally {
                connected = false;
                if (messageListener != null) {
                    javafx.application.Platform.runLater(() ->
                            messageListener.accept(
                                    "OFFLINE|Connexion au serveur perdue."));
                }
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    public void send(String command) {
        if (out != null && connected) {
            out.println(command);
            logger.info("→ ENVOI : " + command);
        } else {
            logger.warning("send() ignoré — non connecté. Commande : " + command);
        }
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    public boolean isConnected() { return connected; }
}