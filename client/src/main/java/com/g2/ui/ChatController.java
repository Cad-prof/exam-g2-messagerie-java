package com.g2.ui;

import com.g2.network.ServerConnection;
import com.g2.dto.Packet;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.g2.model.Message;
import com.g2.model.User;

/**
 * Contrôleur de l'écran de messagerie.
 *
 * ObservableList : liste spéciale JavaFX.
 * Quand on ajoute/retire un élément, le ListView se met à jour
 * automatiquement — pas besoin d'appeler refresh() manuellement.
 */
public class ChatController {

    @FXML private ListView<String>  membersListView;
    @FXML private ListView<String>  messagesListView;
    @FXML private TextArea          messageInput;
    @FXML private Label             chatWithLabel;
    @FXML private Label             errorLabel;

    private User             currentUser;
    private ServerConnection connection;
    private String           selectedMember; // Destinataire actuellement sélectionné

    // ObservableList : liée au ListView, mise à jour automatique de l'UI
    private final ObservableList<String> membersList  = FXCollections.observableArrayList();
    private final ObservableList<String> messagesList = FXCollections.observableArrayList();

    /**
     * Appelé par LoginController après l'ouverture de cet écran.
     * Remplace le initialize() car on a besoin de données externes.
     */
    public void init(User user, ServerConnection connection) {
        this.currentUser = user;
        this.connection  = connection;

        // Rebranche le callback vers CE controller
        connection.setOnPacketReceived(this::handleServerResponse);

        membersListView.setItems(membersList);
        messagesListView.setItems(messagesList);
    }

    /** Sélection d'un membre dans la liste → charge la conversation. */
    @FXML
    private void onMemberSelected() {
        String selected = membersListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals(currentUser.getUsername())) return;

        selectedMember = selected;
        chatWithLabel.setText("Conversation avec " + selectedMember);
        messagesList.clear();
        // TODO Jour 8 : charger l'historique depuis le serveur (GET_HISTORY)
    }

    /** Envoi d'un message (RG7). */
    @FXML
    private void onSend() {
        if (selectedMember == null) {
            showError("Sélectionnez un destinataire.");
            return;
        }

        String contenu = messageInput.getText().trim();

        // RG7 : validations côté client (le serveur valide aussi)
        if (contenu.isEmpty()) {
            showError("Le message ne peut pas être vide.");
            return;
        }
        if (contenu.length() > 1000) {
            showError("Message trop long (max 1000 caractères).");
            return;
        }

        connection.send(Packet.sendMessage(selectedMember, contenu));
        // Affiche le message dans la conversation locale
        messagesList.add("Moi → " + selectedMember + " : " + contenu);
        messageInput.clear();
        errorLabel.setText("");
    }

    /** Déconnexion propre (RG4). */
    @FXML
    private void onLogout() {
        connection.send(Packet.logout());
        connection.disconnect();
        // Retour à l'écran de login
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/login.fxml"));
            javafx.stage.Stage stage =
                    (javafx.stage.Stage) messageInput.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(loader.load(), 400, 350));
            stage.setTitle("G2 — Connexion");
        } catch (Exception e) {
            showError("Erreur retour login : " + e.getMessage());
        }
    }

    /**
     * Traite les Packets reçus du serveur.
     * Platform.runLater() → modifications UI depuis le thread JavaFX.
     */
    private void handleServerResponse(Packet packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {
                // Nouveau message reçu
                case INCOMING_MSG -> {
                    Message msg = packet.getMessage();
                    String sender = msg.getSender().getUsername();
                    messagesList.add(sender + " : " + msg.getContenu());
                    // Scroll automatique vers le bas
                    messagesListView.scrollTo(messagesList.size() - 1);
                }

                // Un utilisateur s'est connecté → ajoute à la liste
                case USER_CONNECTED -> {
                    String username = packet.getUsername();
                    if (!membersList.contains(username)) {
                        membersList.add(username);
                    }
                }

                // Un utilisateur s'est déconnecté → retire de la liste
                case USER_DISCONNECTED ->
                        membersList.remove(packet.getUsername());

                // RG10 : connexion perdue
                case ERROR ->
                        showError(packet.getStatusMessage());
            }
        });
    }

    private void showError(String msg) {
        errorLabel.setText("ERROR: " + msg);
    }
}
