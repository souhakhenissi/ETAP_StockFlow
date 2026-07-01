package org.example.controllers;

import org.example.models.Utilisateur;
import org.example.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class TopbarController {

    @FXML private Label  pageTitle;
    @FXML private Label  notifBadge;
    @FXML private Button themeBtn;
    @FXML private Label  avatarInitials;
    @FXML private Label  userName;
    @FXML private Label  userRole;

    private boolean darkMode = false;

    @FXML
    public void initialize() {
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        if (u != null) {
            userName.setText(u.getNomComplet());
            userRole.setText(u.isAdmin() ? "Administrateur" : "Intervenant");
            String initials = (u.getPrenom().isEmpty() ? "?" : String.valueOf(u.getPrenom().charAt(0)))
                    + (u.getNom().isEmpty() ? "" : String.valueOf(u.getNom().charAt(0)));
            avatarInitials.setText(initials.toUpperCase());
        }
    }

    public void setPageTitle(String title) {
        pageTitle.setText(title);
    }

    public void setNotifCount(int count) {
        notifBadge.setVisible(count > 0);
        notifBadge.setText(count > 9 ? "9+" : String.valueOf(count));
    }

    @FXML
    private void toggleTheme() {
        darkMode = !darkMode;
        Scene scene = themeBtn.getScene();
        if (darkMode) {
            scene.getRoot().getStyleClass().add("dark-theme");
            themeBtn.setText("☀");
        } else {
            scene.getRoot().getStyleClass().remove("dark-theme");
            themeBtn.setText("🌙");
        }
    }

    @FXML
    private void handleNotif() { /* TODO: ouvrir panneau notifications */ }
}