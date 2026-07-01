package org.example.controllers;

import org.example.models.Utilisateur;
import org.example.services.UtilisateurService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

public class UtilisateurFormController {

    // ── Champs formulaire ──────────────────────────────────────────────────────
    @FXML private Label         formTitle;
    @FXML private TextField     nomField, prenomField, emailField;
    @FXML private PasswordField passwordField, confirmField;
    @FXML private ComboBox<String> roleCombo, statutCombo, siteCombo;
    @FXML private Label         mdpLabel;
    @FXML private Button        saveBtn;

    // ── Erreurs inline ─────────────────────────────────────────────────────────
    @FXML private Label errNom, errPrenom, errEmail, errPassword, errConfirm;

    // ── Onglet actif ───────────────────────────────────────────────────────────
    private boolean cameraTabActive = false;

    // ── Webcam thread ──────────────────────────────────────────────────────────
    private OpenCVFrameGrabber   grabber;
    private Java2DFrameConverter converter    = new Java2DFrameConverter();
    private Thread               cameraThread;
    private final AtomicBoolean  cameraActive = new AtomicBoolean(false);
    private volatile BufferedImage lastFrame;

    // ── État ───────────────────────────────────────────────────────────────────
    private Utilisateur utilisateur; // null = création
    private Runnable    onSaved;
    private Runnable    onCancelled;
    private String      faceEncoding; // base64 de la photo choisie

    private final UtilisateurService service = new UtilisateurService();

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("admin", "intervenant"));
        statutCombo.setItems(FXCollections.observableArrayList("actif", "inactif", "bloque"));
        siteCombo.setItems(FXCollections.observableArrayList(
                "Khereddine Pacha", "Mohamed V", "Tunis Centre", "Sfax", "Sousse"));

        // Validation à la volée
        nomField.focusedProperty().addListener((o, ov, nv) -> { if (!nv) validerNom(); });
        prenomField.focusedProperty().addListener((o, ov, nv) -> { if (!nv) validerPrenom(); });
        emailField.focusedProperty().addListener((o, ov, nv) -> { if (!nv) validerEmail(); });
        passwordField.focusedProperty().addListener((o, ov, nv) -> { if (!nv) validerPassword(); });
        confirmField.focusedProperty().addListener((o, ov, nv) -> { if (!nv) validerConfirm(); });
    }

    public void setUtilisateur(Utilisateur u) {
        this.utilisateur = u;
        if (u == null) {
            formTitle.setText("Nouvel utilisateur");
            roleCombo.setValue("intervenant");
            statutCombo.setValue("actif");
            siteCombo.setValue("Khereddine Pacha");
            mdpLabel.setText("Mot de passe *");
        } else {
            formTitle.setText("Modifier — " + u.getNomComplet());
            nomField.setText(u.getNom());
            prenomField.setText(u.getPrenom());
            emailField.setText(u.getEmail());
            roleCombo.setValue(u.getRole().name());
            statutCombo.setValue(u.getStatut().name());
            siteCombo.setValue(u.getSiteAffecte());
            mdpLabel.setText("Nouveau mot de passe (laisser vide = inchangé)");
        }
    }

    public void setOnSaved(Runnable r)     { this.onSaved = r; }
    public void setOnCancelled(Runnable r) { this.onCancelled = r; }


    // ── Validation ─────────────────────────────────────────────────────────────
    private boolean validerNom() {
        if (nomField.getText().trim().isEmpty()) {
            setFieldError(nomField, errNom, "Le nom est obligatoire."); return false;
        }
        clearFieldError(nomField, errNom); return true;
    }
    private boolean validerPrenom() {
        if (prenomField.getText().trim().isEmpty()) {
            setFieldError(prenomField, errPrenom, "Le prénom est obligatoire."); return false;
        }
        clearFieldError(prenomField, errPrenom); return true;
    }
    private boolean validerEmail() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            setFieldError(emailField, errEmail, "L'email est obligatoire."); return false;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            setFieldError(emailField, errEmail, "Format d'email invalide."); return false;
        }
        clearFieldError(emailField, errEmail); return true;
    }
    private boolean validerPassword() {
        String mdp = passwordField.getText();
        if (utilisateur == null && mdp.isEmpty()) {
            setFieldError(passwordField, errPassword, "Le mot de passe est obligatoire."); return false;
        }
        if (!mdp.isEmpty() && mdp.length() < 8) {
            setFieldError(passwordField, errPassword, "Minimum 8 caractères."); return false;
        }
        clearFieldError(passwordField, errPassword); return true;
    }
    private boolean validerConfirm() {
        if (!passwordField.getText().isEmpty()
                && !passwordField.getText().equals(confirmField.getText())) {
            setFieldError(confirmField, errConfirm, "Les mots de passe ne correspondent pas."); return false;
        }
        clearFieldError(confirmField, errConfirm); return true;
    }

    private void setFieldError(Control field, Label errLabel, String msg) {
        field.getStyleClass().removeAll("error"); field.getStyleClass().add("error");
        errLabel.setText(msg); errLabel.setVisible(true); errLabel.setManaged(true);
    }
    private void clearFieldError(Control field, Label errLabel) {
        field.getStyleClass().remove("error");
        errLabel.setVisible(false); errLabel.setManaged(false);
    }

    // ── Sauvegarde ─────────────────────────────────────────────────────────────
    @FXML private void handleSave() {
        boolean valid = validerNom() & validerPrenom() & validerEmail()
                & validerPassword() & validerConfirm();
        if (!valid) return;

        saveBtn.setDisable(true);
        saveBtn.setText("Enregistrement…");

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                if (utilisateur == null) {
                    Utilisateur u = new Utilisateur();
                    u.setNom(nomField.getText().trim());
                    u.setPrenom(prenomField.getText().trim());
                    u.setEmail(emailField.getText().trim().toLowerCase());
                    u.setMotDePasse(passwordField.getText());
                    u.setRole(Utilisateur.Role.valueOf(roleCombo.getValue()));
                    u.setStatut(Utilisateur.Statut.valueOf(statutCombo.getValue()));
                    u.setSiteAffecte(siteCombo.getValue());
                    if (faceEncoding != null) u.setFaceEncoding(faceEncoding);
                    service.creer(u);
                } else {
                    utilisateur.setNom(nomField.getText().trim());
                    utilisateur.setPrenom(prenomField.getText().trim());
                    utilisateur.setEmail(emailField.getText().trim().toLowerCase());
                    utilisateur.setRole(Utilisateur.Role.valueOf(roleCombo.getValue()));
                    utilisateur.setStatut(Utilisateur.Statut.valueOf(statutCombo.getValue()));
                    utilisateur.setSiteAffecte(siteCombo.getValue());
                    if (faceEncoding != null) utilisateur.setFaceEncoding(faceEncoding);
                    service.modifier(utilisateur);
                    if (!passwordField.getText().isEmpty())
                        service.modifierMotDePasse(utilisateur.getId(), passwordField.getText());
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> { if (onSaved != null) onSaved.run(); });
        task.setOnFailed(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveBtn.setText("Enregistrer");
            String msg = task.getException().getMessage();
            if (msg != null && msg.contains("Duplicate"))
                setFieldError(emailField, errEmail, "Cet email est déjà utilisé.");
            else
                setFieldError(emailField, errEmail, "Erreur : " + msg);
        }));

        new Thread(task).start();
    }

    @FXML private void handleCancel() {
        if (onCancelled != null) onCancelled.run();
    }
}
