package sn.isi.chat_messagerie;

import sn.isi.chat_messagerie.client.ServerConnection;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.logging.Logger;

/**
 * Contrôleur de l'écran principal de chat.
 */
public class ChatController {

    private static final Logger logger = Logger.getLogger(ChatController.class.getName());

    @FXML private ListView<String> membersList;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextField messageField;
    @FXML private Label currentChatLabel;
    @FXML private Label connectedUserLabel;
    @FXML private Label statusLabel;
    @FXML private Button listMembersBtn; // Visible uniquement pour ORGANISATEUR

    private String currentUsername;
    private String selectedReceiver;

    private final ServerConnection conn = ServerConnection.getInstance();

    // -------------------------
    // Initialisation avec le nom d'utilisateur
    // -------------------------

    public void init(String username) {
        this.currentUsername = username;
        connectedUserLabel.setText("👤 " + username);

        // Écoute des messages entrants
        conn.setMessageListener(this::handleServerResponse);

        // Masquer le bouton LIST par défaut (RG13)
        listMembersBtn.setVisible(false);

        // Demander la liste des membres au serveur
        conn.send("MEMBERS");
    }

    // -------------------------
    // Sélection d'un membre dans la liste
    // -------------------------

    @FXML
    private void handleSelectMember() {
        String selected = membersList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Nettoyer le préfixe de statut éventuel
        selectedReceiver = selected.replace("🟢 ", "").replace("⚫ ", "").trim().split(" ")[0];
        currentChatLabel.setText("Conversation avec : " + selectedReceiver);
        messagesBox.getChildren().clear();

        // Charger l'historique (RG8)
        conn.send("HISTORY|" + selectedReceiver);
    }

    // -------------------------
    // Envoi d'un message
    // -------------------------

    @FXML
    private void handleSendMessage() {
        if (selectedReceiver == null) {
            showStatus("❌ Sélectionnez un destinataire.", false);
            return;
        }

        String content = messageField.getText().trim();
        if (content.isEmpty()) return;
        if (content.length() > 1000) {
            showStatus("❌ Message trop long (max 1000 caractères).", false);
            return;
        }

        conn.send("SEND|" + selectedReceiver + "|" + content);
        addMessageBubble(content, true); // Afficher localement
        messageField.clear();
    }

    // -------------------------
    // Touche Entrée pour envoyer
    // -------------------------

    @FXML
    private void handleEnterKey(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            handleSendMessage();
        }
    }

    // -------------------------
    // Bouton Déconnexion
    // -------------------------

    @FXML
    private void handleLogout() {
        // Envoyer LOGOUT au serveur
        conn.send("LOGOUT");

        try {
            // Charger la fenêtre de login
            java.net.URL fxmlUrl = getClass().getResource("/sn/isi/chat_messagerie/fxml/Login.fxml");
            if (fxmlUrl == null) fxmlUrl = getClass().getResource("/fxml/Login.fxml");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 420, 520);

            java.net.URL cssUrl = getClass().getResource("/sn/isi/chat_messagerie/css/style.css");
            if (cssUrl == null) cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Messagerie — Association");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            // Fermer la fenêtre de chat
            ((javafx.stage.Stage) messageField.getScene().getWindow()).close();

        } catch (Exception e) {
            showStatus("❌ Erreur déconnexion : " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    // -------------------------
    // Bouton Lister les membres (RG13 — ORGANISATEUR)
    // -------------------------

    @FXML
    private void handleListMembers() {
        conn.send("LIST");
    }

    // -------------------------
    // Traitement des réponses du serveur
    // -------------------------

    private boolean inHistoryMode = false;
    private boolean inListMode = false;

    private void handleServerResponse(String response) {
        String[] parts = response.split("\\|", -1);
        String type = parts[0];

        switch (type) {

            case "OFFLINE" -> {
                // RG10 : perte de connexion au serveur -> afficher erreur et bloquer l'UI
                String msg = parts.length > 1 ? parts[1] : "Connexion perdue.";
                connectedUserLabel.setText("🔴 Hors ligne");
                connectedUserLabel.setStyle("-fx-text-fill: #ef4444;");
                messageField.setDisable(true);
                messageField.setPromptText("Connexion perdue — relancez l'application");
                showStatus("❌ " + msg, false);
                membersList.getItems().clear();
            }

            case "MEMBERS_START" -> {
                membersList.getItems().clear();
            }

            case "MEMBER" -> {
                String uname  = parts.length > 1 ? parts[1] : "?";
                String status = parts.length > 2 ? parts[2] : "OFFLINE";
                String icon   = status.equals("ONLINE") ? "🟢" : "⚫";
                membersList.getItems().add(icon + " " + uname);
            }

            case "MEMBERS_END" -> {
                // Liste chargée — rien à faire
            }

            case "MESSAGE" -> {
                // Nouveau message entrant en temps réel
                String sender  = parts.length > 1 ? parts[1] : "?";
                String content = parts.length > 2 ? parts[2] : "";
                if (sender.equals(selectedReceiver)) {
                    addMessageBubble("[" + sender + "] " + content, false);
                } else {
                    showStatus("💬 Nouveau message de " + sender, true);
                }
            }

            case "HISTORY_START" -> {
                inHistoryMode = true;
                messagesBox.getChildren().clear();
            }

            case "MSG" -> {
                if (inHistoryMode) {
                    String sender  = parts.length > 1 ? parts[1] : "?";
                    String content = parts.length > 2 ? parts[2] : "";
                    boolean isMe   = sender.equals(currentUsername);
                    addMessageBubble(isMe ? content : "[" + sender + "] " + content, isMe);
                }
            }

            case "HISTORY_END" -> inHistoryMode = false;

            case "LIST_START" -> {
                inListMode = true;
                membersList.getItems().clear();
            }

            case "USER" -> {
                if (inListMode) {
                    String uname  = parts.length > 1 ? parts[1] : "?";
                    String role   = parts.length > 2 ? parts[2] : "";
                    String status = parts.length > 3 ? parts[3] : "";
                    String icon   = status.equals("ONLINE") ? "🟢" : "⚫";
                    membersList.getItems().add(icon + " " + uname + " (" + role + ")");
                }
            }

            case "LIST_END" -> inListMode = false;

            case "PENDING" ->
                    showStatus(parts.length > 1 ? parts[1] : "Messages en attente.", true);

            case "OK" ->
                    showStatus(parts.length > 1 ? parts[1] : "OK", true);

            case "ERROR" -> {
                String msg = parts.length > 1 ? parts[1] : "Erreur.";
                showStatus("❌ " + msg, false);
                // RG10 : perte de connexion
                if (msg.contains("connexion perdue")) {
                    connectedUserLabel.setText("🔴 Hors ligne");
                }
                // Activer le bouton LIST si c'est un ORGANISATEUR
                if (msg.contains("RG13") == false && parts[1].contains("ORGANISATEUR")) {
                    listMembersBtn.setVisible(true);
                }
            }

            // Activer le bouton LIST pour les ORGANISATEURS après login réussi
            default -> {
                if (response.contains("ORGANISATEUR")) {
                    listMembersBtn.setVisible(true);
                }
            }
        }

        // Auto-scroll vers le bas
        scrollToBottom();
    }

    // -------------------------
    // Affichage d'une bulle de message
    // -------------------------

    private void addMessageBubble(String content, boolean isMe) {
        HBox row = new HBox();
        row.setPadding(new Insets(4, 12, 4, 12));
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label bubble = new Label(content);
        bubble.getStyleClass().add(isMe ? "bubble-me" : "bubble-other");
        bubble.setWrapText(true);
        bubble.setMaxWidth(360);

        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
    }

    // -------------------------
    // Affichage d'un statut
    // -------------------------

    private void showStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle(success
                ? "-fx-text-fill: #27ae60;"
                : "-fx-text-fill: #e74c3c;");
    }

    // -------------------------
    // Scroll automatique
    // -------------------------

    private void scrollToBottom() {
        javafx.application.Platform.runLater(() ->
                messagesScrollPane.setVvalue(1.0)
        );
    }
}