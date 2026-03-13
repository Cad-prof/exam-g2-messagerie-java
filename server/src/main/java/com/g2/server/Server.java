package com.g2.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Serveur principal — écoute les connexions entrantes
 * et crée un thread par client (RG11).
 */
public class Server {

    private static final int PORT = 8088;
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    // Map des clients connectés : username -> handler (RG3)
    private static final ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        logger.info("Démarrage du serveur sur le port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Serveur prêt. En attente de connexions...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Nouvelle connexion : " + clientSocket.getInetAddress());

                // Un thread par client (RG11)
                ClientHandler handler = new ClientHandler(clientSocket, connectedClients);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }

        } catch (IOException e) {
            logger.severe("Erreur serveur : " + e.getMessage());
        }
    }

    // Méthodes utilitaires

    public static ConcurrentHashMap<String, ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    public static void removeClient(String username) {
        connectedClients.remove(username);
        logger.info("Client déconnecté et retiré : " + username);
    }
}