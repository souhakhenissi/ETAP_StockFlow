package org.example.controllers;

import javafx.application.Platform;
import org.example.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

public class AdminDashboardController {

    // Contrôleurs injectés via fx:include — JAMAIS rechargés
    @FXML private SidebarController sidebarController;
    @FXML private TopbarController  topbarController;

    // Zone qui change à chaque navigation
    @FXML private StackPane contentPane;

    private static final Map<String, String[]> ROUTES = Map.ofEntries(
            Map.entry("btnDashboard",    new String[]{"admin/dashboard.fxml",              "Tableau de bord"}),
            Map.entry("btnMateriels",    new String[]{"admin/materiels.fxml",            "Matériels"}),
            Map.entry("btnStock",        new String[]{"admin/stock.fxml",                "Stock"}),
            Map.entry("btnDemandes",     new String[]{"admin/demandes.fxml",             "Demandes"}),
            Map.entry("btnAffectations", new String[]{"admin/affectations.fxml",         "Affectations"}),
            Map.entry("btnFournisseurs", new String[]{"admin/fournisseurs.fxml",         "Fournisseurs"}),
            Map.entry("btnRecherche",    new String[]{"admin/recherche.fxml",            "Recherche"}),
            Map.entry("btnRapports",     new String[]{"admin/rapports.fxml",             "Rapports"}),
            Map.entry("btnUtilisateurs", new String[]{"admin/gestion_utilisateurs.fxml", "Utilisateurs"}),
            Map.entry("btnProfil",       new String[]{"admin/profil.fxml",               "Mon profil"}),
            Map.entry("btnParametres",   new String[]{"admin/parametres.fxml",           "Paramètres"})
    );

    @FXML
    public void initialize() {
        // Lier la navigation de la sidebar à ce contrôleur
        sidebarController.setNavigationHandler(this::navigateTo);
        sidebarController.setLogoutHandler(this::handleLogout);

        // Page par défaut au démarrage
        navigateTo("btnUtilisateurs");
        sidebarController.setActiveButton("btnUtilisateurs");

        Platform.runLater(() -> {
            sidebarController.getRoot().requestLayout();
            // si vous avez accès au conteneur parent
            sidebarController.getRoot().getParent().requestLayout();
        });

    }

    /**
     * Charge uniquement la vue dans contentPane.
     * La sidebar et la topbar restent intactes.
     */
    private void navigateTo(String buttonId) {
        String[] route = ROUTES.get(buttonId);
        if (route == null) return;

        String fxmlPath = route[0];
        String title    = route[1];

        // Mettre à jour le titre de la topbar sans la recharger
        topbarController.setPageTitle(title);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/" + fxmlPath));
            Node view = loader.load();

            // Remplacer uniquement le contenu — pas toute la fenêtre
            contentPane.getChildren().setAll(view);

        } catch (IOException e) {
            System.err.println("[Navigation] Vue introuvable : " + fxmlPath);
            // Afficher une page vide plutôt que planter
        }
    }

    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/login.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/styles/main.css").toExternalForm());
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.setWidth(1100);
            stage.setHeight(700);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}