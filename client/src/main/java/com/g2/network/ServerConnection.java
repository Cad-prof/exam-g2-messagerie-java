package com.g2.network;

import com.g2.dto.Packet;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Gère la connexion socket côté client.
 *
 * Le constructeur reçoit un Consumer<Packet> : une fonction qui sera
 * appelée à chaque Packet reçu du serveur. Le code réseau ne connaît
 * pas l'UI — il délègue le traitement au caller (LoginController, ChatController).
 */
public class ServerConnection {

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private boolean            connected = false;

    private final Consumer<Packet> onPacketReceived;

    public ServerConnection(Consumer<Packet> onPacketReceived) {
        this.onPacketReceived = onPacketReceived;
    }

    /**
     * Établit la connexion avec le serveur.
     * host : "localhost" en local, "192.168.x.x" sur le LAN.
     * port : 5000 (doit correspondre au serveur).
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        // out AVANT in (même règle que côté serveur)
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
        connected = true;

        // Thread d'écoute en arrière-plan
        Thread listener = new Thread(this::listenLoop);
        listener.setDaemon(true); // S'arrête quand l'app JavaFX se ferme
        listener.start();
    }

    /** Boucle de réception des Packets entrants. */
    private void listenLoop() {
        try {
            Packet packet;
            while ((packet = (Packet) in.readObject()) != null) {
                onPacketReceived.accept(packet); // Délègue au controller
            }
        } catch (IOException | ClassNotFoundException e) {
            // RG10 : connexion perdue → notifie l'UI
            connected = false;
            onPacketReceived.accept(Packet.error("Connexion au serveur perdue."));
        }
    }

    /** Envoie un Packet au serveur. */
    public synchronized void send(Packet packet) {
        if (!connected) return;
        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
        } catch (IOException e) {
            connected = false;
            onPacketReceived.accept(Packet.error("Impossible d'envoyer le message."));
        }
    }

    /** Ferme proprement la connexion. */
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}