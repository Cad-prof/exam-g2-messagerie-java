package com.g2.ui;

import com.g2.network.ServerConnection;
import com.g2.dto.Packet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.g2.model.User;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Contrôleur de l'écran de connexion/inscription.
 * @FXML : annotation qui lie les variables aux éléments du fichier login.fxml.
 */
public class LoginController {

    @FXML private TextField    serverIpField;
    @FXML private TabPane      tabPane;
    @FXML private TextField    loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private TextField    registerUsernameField;
    @FXML private PasswordField registerPasswordField;
    @FXML private ComboBox<User.Role> roleComboBox;
    @FXML private Label        statusLabel;

    private ServerConnection connection;

    /**
     * Appelé automatiquement par JavaFX après le chargement du FXML.
     * Équivalent d'un constructeur pour les contrôleurs FXML.
     */
    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll(User.Role.values());
        roleComboBox.setValue(User.Role.MEMBRE);

        // Charge l'IP depuis config.properties
        serverIpField.setText(loadServerHost());

        // Crée la connexion avec le callback de réception
        connection = new ServerConnection(this::handleServerResponse);
    }

    /** Lit le fichier config.properties pour l'IP du serveur. */
    private String loadServerHost() {
        try {
            Properties config = new Properties();
            config.load(new FileInputStream("config.properties"));
            return config.getProperty("server.host", "localhost");
        } catch (Exception e) {
            return "localhost"; // Valeur par défaut si fichier absent
        }
    }

    /** Connexion au serveur puis envoi du Packet LOGIN. */
    @FXML
    private void onLogin() {
        String host     = serverIpField.getText().trim();
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Veuillez remplir tous les champs.", true);
            return;
        }

        connectIfNeeded(host);
        connection.send(Packet.login(username, password));
    }

    /** Connexion au serveur puis envoi du Packet REGISTER. */
    @FXML
    private void onRegister() {
        String host     = serverIpField.getText().trim();
        String username = registerUsernameField.getText().trim();
        String password = registerPasswordField.getText();
        User.Role role  = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Veuillez remplir tous les champs.", true);
            return;
        }

        connectIfNeeded(host);
        connection.send(Packet.register(username, password, role));
    }

    /** Connecte au serveur si pas encore connecté. */
    private void connectIfNeeded(String host) {
        if (!connection.isConnected()) {
            try {
                connection.connect(host, 5000);
            } catch (Exception e) {
                showStatus("Impossible de joindre " + host + ":8088", true);
            }
        }
    }

    /**
     * Traite les Packets reçus du serveur.
     * APPELÉ DEPUIS LE THREAD RÉSEAU → Platform.runLater() obligatoire
     * pour modifier l'UI depuis le thread JavaFX.
     */
    private void handleServerResponse(Packet packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {
                case LOGIN_SUCCESS -> openChatScreen(packet.getUser());
                case SUCCESS       -> showStatus("SECCUSS: " + packet.getStatusMessage(), false);
                case ERROR         -> showStatus("ERROR: " + packet.getStatusMessage(), true);
            }
        });
    }

    /** Ouvre l'écran de chat et passe l'utilisateur + la connexion. */
    private void openChatScreen(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat.fxml"));
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 800, 600));

            // Passe les données au ChatController
            ChatController chatController = loader.getController();
            chatController.init(user, connection);

            stage.setTitle("G2 Messagerie — " + user.getUsername());
        } catch (Exception e) {
            showStatus("Erreur ouverture chat : " + e.getMessage(), true);
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
```

        ---