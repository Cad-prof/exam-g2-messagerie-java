package com.g2.contoller;

import com.g2.network.ServerConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class LoginController {

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField        usernameField;
    @FXML private PasswordField    passwordField;
    @FXML private Label            statusLabel;

    @FXML private TextField        usernameRegField;
    @FXML private PasswordField    passwordRegField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label            statusRegLabel;

    @FXML private TabPane tabPane;

    private final ServerConnection conn = ServerConnection.getInstance();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (roleComboBox != null) {
                roleComboBox.getItems().setAll("MEMBRE", "BENEVOLE", "ORGANISATEUR");
                roleComboBox.getSelectionModel().selectFirst();
            }
        });
    }

    // Connexion

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Remplissez tous les champs.");
            return;
        }

        try {
            conn.connect();
            conn.setMessageListener(response ->
                    Platform.runLater(() -> handleLoginResponse(response, username))
            );
            conn.send("LOGIN|" + username + "|" + password);
        } catch (Exception e) {
            showError("Connexion serveur impossible : " + e.getMessage());
        }
    }

    private void handleLoginResponse(String response, String username) {
        String[] parts = response.split("\\|", -1);
        String   type  = parts[0];

        if ("ERROR".equals(type)) {
            String msg = parts.length > 1 ? parts[1] : "Identifiants incorrects.";
            showError("❌ " + msg);
            return;
        }

        if ("OK".equals(type) && parts.length > 2 && "LOGIN".equals(parts[1])) {
            String role = parts[2]; // OK|LOGIN|ORGANISATEUR

            logger.info("[RG12] Login OK : " + username + " role=" + role);

            // IMPORTANT : couper le listener AVANT d'ouvrir le chat
            // pour que les messages suivants (MEMBERS_START etc.)
            // soient reçus par le ChatController et non par ce listener
            conn.setMessageListener(null);

            openChatWindow(username, role);
        }
        // Ignorer silencieusement tout le reste (MEMBERS_START, MEMBER, etc.)
    }

    //  Inscription

    @FXML
    private void handleRegister() {
        String username = usernameRegField.getText().trim();
        String password = passwordRegField.getText();
        String role     = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showRegError("Remplissez tous les champs.");
            return;
        }
        if (password.length() < 4) {
            showRegError("Mot de passe trop court (min 4 caractères).");
            return;
        }

        try {
            conn.connect();
            conn.setMessageListener(response -> Platform.runLater(() -> {
                String[] parts = response.split("\\|", -1);
                if ("OK".equals(parts[0])) {
                    conn.setMessageListener(null); // couper après réponse
                    showRegSuccess("✅ Compte créé ! Connectez-vous.");
                } else {
                    showRegError(" " + (parts.length > 1 ? parts[1] : "Erreur inscription."));
                }
            }));
            conn.send("REGISTER|" + username + "|" + password + "|" + role);
        } catch (Exception e) {
            showRegError("Connexion serveur impossible : " + e.getMessage());
        }
    }

    //  Ouverture du Chat

    private void openChatWindow(String username, String role) {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/com/g2/hello-view.fxml");
            if (fxmlUrl == null) fxmlUrl = getClass().getResource("/com/g2/hello-view.fxml");
            if (fxmlUrl == null) fxmlUrl = getClass().getResource("/com/g2/hello-view.fxmll");
            if (fxmlUrl == null) {
                showError("Erreur : fichier FXML du chat introuvable.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene      scene  = new Scene(loader.load(), 960, 640);

            java.net.URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl == null) cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            ChatController chatController = loader.getController();
            // Le ChatController installe son propre listener dans init()
            chatController.init(username, role);

            Stage chatStage = new Stage();
            chatStage.setTitle("Messagerie — " + username + " (" + role + ")");
            chatStage.setScene(scene);
            chatStage.setOnCloseRequest(e -> conn.disconnect());
            chatStage.show();

            ((Stage) usernameField.getScene().getWindow()).close();

        } catch (Exception e) {
            showError("Erreur : " + e.getClass().getSimpleName() + " — " + e.getMessage());
            e.printStackTrace();
        }
    }

    //  Helpers UI

    private void showError(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill:#ef4444;");
        }
    }

    private void showRegError(String msg) {
        if (statusRegLabel != null) {
            statusRegLabel.setText(msg);
            statusRegLabel.setStyle("-fx-text-fill:#ef4444;");
        }
    }

    private void showRegSuccess(String msg) {
        if (statusRegLabel != null) {
            statusRegLabel.setText(msg);
            statusRegLabel.setStyle("-fx-text-fill:#22c55e;");
        }
    }
}