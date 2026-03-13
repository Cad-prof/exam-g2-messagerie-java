package com.g2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application client JavaFX.
 * Application.launch() démarre le moteur JavaFX et appelle start().
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        primaryStage.setScene(new Scene(loader.load(), 400, 350));
        primaryStage.setTitle("G2 — Connexion");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
