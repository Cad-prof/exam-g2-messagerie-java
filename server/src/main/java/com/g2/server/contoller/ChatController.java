package com.g2.server.contoller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.g2.network.ServerConnection;

import java.util.logging.Logger;

/**
 * Droits par rôle :
 *  ORGANISATEUR → LIST (tous les membres inscrits, RG13) + chat 1:1 avec tous
 *  MEMBRE       → MEMBERS (membres + organisateurs) + chat 1:1
 *  BENEVOLE     → pas de contacts, pas de chat 1:1
 */
public class ChatController {

    private static final Logger logger = Logger.getLogger(ChatController.class.getName());

    // ── TopBar
    @FXML private Label  connectedUserLabel;
    @FXML private Label  roleLabel;
    @FXML private Button listAllBtn;

    // ── Sidebar
    @FXML private Label            sidebarTitle;
    @FXML private ListView<String> membersList;
    @FXML private Label            sidebarInfo;

    // ── Zone chat
    @FXML private Label      currentChatLabel;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox       messagesBox;
    @FXML private Label      statusLabel;
    @FXML private TextField  messageField;
    @FXML private Button     sendBtn;

    // ── État
    private String  currentUsername;
    private String  currentRole;
    private String  selectedReceiver;
    private boolean inHistoryMode = false;
    private boolean inListMode    = false;

    private final ServerConnection conn = ServerConnection.getInstance();

    // init() — appelé depuis LoginController


    public void init(String username, String role) {
        this.currentUsername = username;
        this.currentRole     = role;

        conn.setMessageListener(msg -> Platform.runLater(() -> handleServerResponse(msg)));

        connectedUserLabel.setText("👤 " + username);
        roleLabel.setText(role);
        styleRoleLabel(role);
        applyRoleRestrictions(role);

        // Charger les contacts selon le rôle
        if ("ORGANISATEUR".equals(role)) {
            conn.send("LIST");          // RG13 : liste complète
        } else if ("MEMBRE".equals(role)) {
            conn.send("MEMBERS");       // membres + organisateurs
        }
        // BENEVOLE : rien à charger
    }

    public void init(String username) {
        init(username, "MEMBRE");
    }

    // Adapter l'interface selon le rôle


    private void applyRoleRestrictions(String role) {
        switch (role) {

            case "ORGANISATEUR" -> {
                listAllBtn.setVisible(true);
                listAllBtn.setManaged(true);
                sidebarTitle.setText("Tous les membres inscrits");
                sidebarInfo.setVisible(false);
                sidebarInfo.setManaged(false);
                membersList.setVisible(true);
                membersList.setManaged(true);
                messageField.setDisable(false);
                messageField.setPromptText("Écrivez votre message...");
                sendBtn.setDisable(false);
                currentChatLabel.setText("📋 Liste complète des membres");
            }

            case "MEMBRE" -> {
                listAllBtn.setVisible(false);
                listAllBtn.setManaged(false);
                sidebarTitle.setText("Membres & Organisateurs");
                sidebarInfo.setVisible(false);
                sidebarInfo.setManaged(false);
                membersList.setVisible(true);
                membersList.setManaged(true);
                messageField.setDisable(false);
                messageField.setPromptText("Écrivez votre message...");
                sendBtn.setDisable(false);
                currentChatLabel.setText("Sélectionnez un contact pour démarrer");
            }

            case "BENEVOLE" -> {
                listAllBtn.setVisible(false);
                listAllBtn.setManaged(false);
                sidebarTitle.setText("Contacts");
                sidebarInfo.setText("ℹ️ Les bénévoles n'ont pas de contacts directs.");
                sidebarInfo.setVisible(true);
                sidebarInfo.setManaged(true);
                membersList.setVisible(false);
                membersList.setManaged(false);
                messageField.setDisable(true);
                messageField.setPromptText("Chat 1:1 non disponible pour les bénévoles");
                sendBtn.setDisable(true);
                currentChatLabel.setText("Mode bénévole — consultation uniquement");
            }
        }
    }

    private void styleRoleLabel(String role) {
        String color = switch (role) {
            case "ORGANISATEUR" -> "#f59e0b";
            case "MEMBRE"       -> "#3b82f6";
            case "BENEVOLE"     -> "#6b7280";
            default             -> "#ffffff";
        };
        roleLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;-fx-font-weight:bold;");
    }


    // Sélection membre → historique (RG8)

    @FXML
    private void handleSelectMember() {
        if ("BENEVOLE".equals(currentRole)) return;

        String selected = membersList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Format : "🟢 username [ROLE]" → extraire username
        String cleaned = selected
                .replace("🟢 ", "").replace("⚫ ", "").trim();
        selectedReceiver = cleaned.contains(" ") ? cleaned.split(" ")[0] : cleaned;

        currentChatLabel.setText("💬 Conversation avec : " + selectedReceiver);
        messagesBox.getChildren().clear();
        conn.send("HISTORY|" + selectedReceiver);
    }

    // Envoi de message — RG5, RG7

    @FXML
    private void handleSendMessage() {
        if ("BENEVOLE".equals(currentRole)) {
            showStatus("❌ Les bénévoles ne peuvent pas envoyer de messages privés.", false);
            return;
        }
        if (selectedReceiver == null) {
            showStatus(" Sélectionnez un destinataire.", false);
            return;
        }
        String content = messageField.getText().trim();
        if (content.isEmpty()) return;
        if (content.length() > 1000) {
            showStatus(" Message trop long (max 1000 caractères). [RG7]", false);
            return;
        }
        conn.send("SEND|" + selectedReceiver + "|" + content);
        addBubble(content, true);
        messageField.clear();
    }

    @FXML
    private void handleEnterKey(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) handleSendMessage();
    }

    // Déconnexion → fermer chat et rouvrir la page de connexion

    @FXML
    private void handleLogout() {
        conn.send("LOGOUT");
        conn.disconnect();

        try {
            java.net.URL fxmlUrl = getClass().getResource("/com/g2/login.fxml");
            if (fxmlUrl == null) fxmlUrl = getClass().getResource("/com/g2/login.fxm");
            if (fxmlUrl == null) {
                showStatus("❌ Login.fxml introuvable.", false);
                return;
            }

            FXMLLoader loader  = new FXMLLoader(fxmlUrl);
            Scene      scene   = new Scene(loader.load(), 480, 560);

            java.net.URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl == null) cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            Stage loginStage = new Stage();
            loginStage.setTitle("Messagerie — Association");
            loginStage.setScene(scene);
            loginStage.setResizable(false);
            loginStage.show();

            logger.info("[RG12] Déconnexion : " + currentUsername + " → retour page connexion");

            // Fermer la fenêtre chat
            ((Stage) messageField.getScene().getWindow()).close();

        } catch (Exception ex) {
            showStatus("❌ Erreur retour connexion : " + ex.getMessage(), false);
            ex.printStackTrace();
        }
    }

    // RG13 — Liste complète (bouton ORGANISATEUR)

    @FXML
    private void handleListAll() {
        if (!"ORGANISATEUR".equals(currentRole)) {
            showStatus("❌ Accès réservé aux organisateurs.", false);
            return;
        }
        membersList.getItems().clear();
        currentChatLabel.setText(" Liste complète des membres inscrits");
        inListMode = true;
        conn.send("LIST");
    }

    // Traitement des réponses serveur

    private void handleServerResponse(String response) {
        String[] parts = response.split("\\|", -1);
        String   type  = parts[0];

        switch (type) {

            // RG10 — perte de connexion
            case "OFFLINE" -> {
                String msg = parts.length > 1 ? parts[1] : "Connexion perdue.";
                connectedUserLabel.setText(" Hors ligne");
                connectedUserLabel.setStyle("-fx-text-fill:#ef4444;");
                messageField.setDisable(true);
                messageField.setPromptText("Connexion perdue — relancez l'application");
                sendBtn.setDisable(true);
                showStatus("❌ " + msg, false);
                membersList.getItems().clear();
            }

            // Mise à jour du rôle
            case "ROLE" -> {
                String role = parts.length > 1 ? parts[1] : "MEMBRE";
                this.currentRole = role;
                roleLabel.setText(role);
                styleRoleLabel(role);
                applyRoleRestrictions(role);
            }

            // Liste filtrée MEMBRES + ORGANISATEURS
            case "MEMBERS_START" -> membersList.getItems().clear();

            case "MEMBER" -> {
                String uname  = parts.length > 1 ? parts[1] : "?";
                String status = parts.length > 2 ? parts[2] : "OFFLINE";
                String mrole  = parts.length > 3 ? parts[3] : "";
                String icon   = "ONLINE".equals(status) ? "🟢" : "Déconnecté |";
                membersList.getItems().add(icon + " " + uname + " [" + mrole + "]");
            }

            case "MEMBERS_END" -> { /* fin liste */ }

            // RG13 — liste complète (ORGANISATEUR)
            case "LIST_START" -> {
                inListMode = true;
                membersList.getItems().clear();
                String count = parts.length > 1 ? parts[1] : "?";
                currentChatLabel.setText(" Liste complète — " + count + " membres inscrits");
            }

            case "USER" -> {
                if (!inListMode) break;
                String uname   = parts.length > 1 ? parts[1] : "?";
                String urole   = parts.length > 2 ? parts[2] : "";
                String ustatus = parts.length > 3 ? parts[3] : "OFFLINE";
                String icon    = "ONLINE".equals(ustatus) ? "🟢" : "Déconnecté |";
                membersList.getItems().add(icon + " " + uname + " [" + urole + "]");
            }

            case "LIST_END" -> {
                inListMode = false;
                logger.info("[RG13] Liste complète chargée pour " + currentUsername);
            }

            // Messages 1:1
            case "MESSAGE" -> {
                String sender  = parts.length > 1 ? parts[1] : "?";
                String content = parts.length > 2 ? parts[2] : "";
                if (sender.equals(selectedReceiver)) {
                    addBubble(content, false);
                } else {
                    showStatus(" Nouveau message de " + sender, true);
                }
            }

            // Historique RG8
            case "HISTORY_START" -> {
                inHistoryMode = true;
                messagesBox.getChildren().clear();
            }

            case "MSG" -> {
                if (!inHistoryMode) break;
                String sender  = parts.length > 1 ? parts[1] : "?";
                String content = parts.length > 2 ? parts[2] : "";
                addBubble(content, sender.equals(currentUsername));
            }

            case "HISTORY_END" -> inHistoryMode = false;

            // Messages en attente RG6
            case "PENDING" -> {
                String sender  = parts.length > 1 ? parts[1] : "?";
                String content = parts.length > 2 ? parts[2] : "";
                showStatus(" Message en attente de " + sender, true);
                if (sender.equals(selectedReceiver)) addBubble(content, false);
            }

            // Erreurs et confirmations
            case "ERROR" -> showStatus(" " + (parts.length > 1 ? parts[1] : "Erreur."), false);
            case "OK"    -> { if (parts.length > 1) showStatus("Ok " + parts[1], true); }
        }
    }

    // Bulles de message

    private void addBubble(String text, boolean isMe) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);
        bubble.getStyleClass().add(isMe ? "bubble-me" : "bubble-other");

        HBox row = new HBox(bubble);
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 10, 3, 10));

        messagesBox.getChildren().add(row);
        messagesScrollPane.layout();
        messagesScrollPane.setVvalue(1.0);
    }

    // Barre de statut

    private void showStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
                "-fx-text-fill:" + (success ? "#22c55e" : "#ef4444") + ";" +
                        "-fx-font-size:11px;"
        );
    }
}