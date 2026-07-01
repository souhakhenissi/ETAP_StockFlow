package org.example.controllers;

import org.example.models.Utilisateur;
import org.example.services.UtilisateurService;
import org.example.utils.CaptchaServer;
import org.example.utils.PasswordUtil;
import org.example.utils.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    // ── Formulaire ─────────────────────────────────────────────────────────────
    @FXML private TextField          emailField;
    @FXML private PasswordField      passwordField;
    @FXML private TextField          passwordVisibleField;
    @FXML private Button             togglePasswordBtn;
    @FXML private Button             loginButton;
    @FXML private Label              errorLabel;
    @FXML private Label              emailErrorLabel;
    @FXML private Label              passwordErrorLabel;
    @FXML private ProgressIndicator  spinner;



    private boolean  passwordVisible     = false;
    private String   currentToken        = "";


    private final UtilisateurService service = new UtilisateurService();

    // ══════════════════════════════════════════════════════════════════════════
    // INITIALISATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        spinner.setVisible(false);
        hideError(errorLabel);
        hideError(emailErrorLabel);
        hideError(passwordErrorLabel);

        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

    }

    // ══════════════════════════════════════════════════════════════════════════
    // TOGGLE MOT DE PASSE
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            togglePasswordBtn.setText("👁");
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ══════════════════════════════════════════════════════════════════════════

    private boolean validerFormulaire() {
        boolean valide = true;

        String email = emailField.getText().trim();
        String mdp   = passwordField.getText();

        if (email.isEmpty()) {
            showFieldError(emailErrorLabel, "L'adresse e-mail est requise.");
            valide = false;
        } else if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            showFieldError(emailErrorLabel, "Veuillez saisir une adresse e-mail valide.");
            valide = false;
        } else {
            hideError(emailErrorLabel);
        }

        if (mdp.isEmpty()) {
            showFieldError(passwordErrorLabel, "Le mot de passe est requis.");
            valide = false;
        } else {
            hideError(passwordErrorLabel);
        }

        return valide;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTION LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleLogin() {
        hideError(errorLabel);
        if (!validerFormulaire()) return;

        String email = emailField.getText().trim();
        String mdp   = passwordField.getText();

        spinner.setVisible(true);
        loginButton.setDisable(true);

        Task<Utilisateur> task = new Task<>() {
            @Override
            protected Utilisateur call() throws Exception {
                return service.authentifier(email, mdp);
            }
        };

        task.setOnSucceeded(e -> {
            spinner.setVisible(false);
            loginButton.setDisable(false);
            Utilisateur u = task.getValue();
            if (u != null) {
                SessionManager.getInstance().setUtilisateurConnecte(u);
                ouvrirDashboard(u);
            } else {
                showError("Email ou mot de passe incorrect.");
            }
        });

        task.setOnFailed(e -> {
            spinner.setVisible(false);
            loginButton.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof SQLException sqle) {
                switch (sqle.getMessage()) {
                    case "COMPTE_BLOQUE"  -> showError("Votre compte est bloqué. Contactez l'administrateur.");
                    case "COMPTE_INACTIF" -> showError("Votre compte est désactivé.");
                    default               -> showError("Erreur de connexion : " + sqle.getMessage());
                }
            } else {
                showError("Erreur inattendue. Réessayez.");
            }
        });

        new Thread(task).start();
    }

    private void ouvrirDashboard(Utilisateur u) {
        try {
            String fxml = u.isAdmin()
                    ? "/views/admin/dashboard.fxml"
                    : "/views/intervenant/dashboard.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/styles/main.css").toExternalForm());
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("ETAP StockFlow — " +
                    (u.isAdmin() ? "Administration" : "Espace Intervenant"));
            stage.setMaximized(true);
        } catch (IOException ex) {
            showError("Impossible de charger le tableau de bord.");
            ex.printStackTrace();
        }
    }
    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS UI
    // ══════════════════════════════════════════════════════════════════════════

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError(Label label) {
        if (label != null) {
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void showFieldError(Label label, String msg) {
        if (label != null) {
            label.setText(msg);
            label.setVisible(true);
            label.setManaged(true);
        }
    }
}