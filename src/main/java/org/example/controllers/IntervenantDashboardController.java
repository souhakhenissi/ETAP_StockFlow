package org.example.controllers;

import org.example.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

public class IntervenantDashboardController {

    @FXML private SidebarIntervenantController sidebarIntervenantController;
    @FXML private TopbarController             topbarController;
    @FXML private StackPane                    contentPane;

    private static final Map<String, String[]> ROUTES = Map.of(
            "btnDashboard",    new String[]{"intervenant/accueil_intervenant.fxml",  "Tableau de bord"},
            "btnDemandes",     new String[]{"intervenant/demandes_intervenant.fxml", "Mes demandes"},
            "btnAffectations", new String[]{"intervenant/affectations_intervenant.fxml", "Mes affectations"},
            "btnProfil",       new String[]{"intervenant/profil_intervenant.fxml",   "Mon profil"}
    );

    @FXML
    public void initialize() {
        sidebarIntervenantController.setNavigationHandler(this::navigateTo);
        sidebarIntervenantController.setLogoutHandler(this::handleLogout);

        // Mettre à jour le badge de notifications
        rafraichirNotifications();

        // Page par défaut
        navigateTo("btnDemandes");
        sidebarIntervenantController.setActiveButton("btnDemandes");
    }

    private void navigateTo(String buttonId) {
        String[] route = ROUTES.get(buttonId);
        if (route == null) return;
        topbarController.setPageTitle(route[1]);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/" + route[0]));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("[Intervenant] Vue introuvable : " + route[0]);
        }
    }

    private void rafraichirNotifications() {
        try {
            var service = new org.example.services.DemandeService();
            int n = service.countNotifsNonLues(
                    SessionManager.getInstance().getUtilisateurConnecte().getId());
            topbarController.setNotifCount(n);
        } catch (Exception ignored) {}
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
