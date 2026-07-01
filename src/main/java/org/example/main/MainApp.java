package org.example.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/login.fxml"));
        Scene scene = new Scene(loader.load()); // pas besoin de dimensions fixes
        scene.getStylesheets().add(
                getClass().getResource("/styles/main.css").toExternalForm());

        primaryStage.setTitle("ETAP StockFlow — Connexion");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // ⚡ Passer en plein écran
        primaryStage.setFullScreen(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
