package org.example.controllers;

import org.example.models.Fournisseur;
import org.example.services.FournisseurService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.kordamp.ikonli.javafx.FontIcon;

public class FournisseurFormController {

    @FXML private Label    formTitle;
    @FXML private TextField nomField, emailField, telephoneField, adresseField;
    @FXML private ComboBox<String> specialiteCombo;
    @FXML private Label    errNom, errEmail, errTelephone;
    @FXML private Button   saveBtn;
    @FXML private Label    saveBtnLabel;
    @FXML private FontIcon saveIcon;

    private Fournisseur fournisseur; // null = création
    private Runnable    onSaved;
    private Runnable    onCancelled;

    private final FournisseurService service = new FournisseurService();

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        specialiteCombo.setItems(FXCollections.observableArrayList(
                "Équipements réseau",
                "Câblage et connectique",
                "Serveurs et stockage",
                "Consommables informatique",
                "Téléphonie IP",
                "Sécurité réseau",
                "Onduleurs et énergie",
                "Logiciels et licences",
                "Mobilier et bureau",
                "Autre"
        ));

        // Validation à la volée
        nomField.focusedProperty().addListener((o, ov, nv) -> { if (!nv) validerNom(); });
        emailField.focusedProperty().addListener((o, ov, nv) -> { if (!nv && !emailField.getText().isBlank()) validerEmail(); });
        telephoneField.focusedProperty().addListener((o, ov, nv) -> { if (!nv && !telephoneField.getText().isBlank()) validerTelephone(); });
    }

    public void setFournisseur(Fournisseur f) {
        this.fournisseur = f;
        if (f == null) {
            formTitle.setText("Nouveau fournisseur");
            saveBtnLabel.setText("Ajouter");
        } else {
            formTitle.setText("Modifier — " + f.getNom());
            nomField.setText(f.getNom());
            emailField.setText(f.getEmail() != null ? f.getEmail() : "");
            telephoneField.setText(f.getTelephone() != null ? f.getTelephone() : "");
            adresseField.setText(f.getAdresse() != null ? f.getAdresse() : "");
            if (f.getSpecialite() != null && !f.getSpecialite().isBlank()) {
                // Si la valeur est dans la liste, la sélectionner ; sinon l'écrire
                if (specialiteCombo.getItems().contains(f.getSpecialite()))
                    specialiteCombo.setValue(f.getSpecialite());
                else
                    specialiteCombo.getEditor().setText(f.getSpecialite());
            }
            saveBtnLabel.setText("Enregistrer");
        }
    }

    public void setOnSaved(Runnable r)     { this.onSaved = r; }
    public void setOnCancelled(Runnable r) { this.onCancelled = r; }

    // ── Validation ─────────────────────────────────────────────────────────────
    private boolean validerNom() {
        if (nomField.getText().trim().isEmpty()) {
            setError(nomField, errNom, "Le nom est obligatoire.");
            return false;
        }
        clearError(nomField, errNom); return true;
    }

    private boolean validerEmail() {
        String email = emailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            setError(emailField, errEmail, "Format d'email invalide.");
            return false;
        }
        clearError(emailField, errEmail); return true;
    }

    private boolean validerTelephone() {
        String tel = telephoneField.getText().trim();
        if (!tel.isEmpty() && !tel.matches("^[+\\d\\s()-]{7,20}$")) {
            setError(telephoneField, errTelephone, "Format de téléphone invalide.");
            return false;
        }
        clearError(telephoneField, errTelephone); return true;
    }

    private void setError(Control field, Label errLabel, String msg) {
        field.getStyleClass().remove("error");
        field.getStyleClass().add("error");
        errLabel.setText(msg);
        errLabel.setVisible(true);
        errLabel.setManaged(true);
    }

    private void clearError(Control field, Label errLabel) {
        field.getStyleClass().remove("error");
        errLabel.setVisible(false);
        errLabel.setManaged(false);
    }

    // ── Sauvegarde ─────────────────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        boolean valid = validerNom() & validerEmail() & validerTelephone();
        if (!valid) return;

        saveBtn.setDisable(true);
        saveBtnLabel.setText("Enregistrement…");
        saveIcon.setIconLiteral("mdi2l-loading:14");

        // Récupérer la spécialité (liste ou saisie libre)
        String specialite = specialiteCombo.getValue() != null
                ? specialiteCombo.getValue()
                : specialiteCombo.getEditor().getText();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                if (fournisseur == null) {
                    Fournisseur f = new Fournisseur(
                            nomField.getText().trim(),
                            emailField.getText().trim(),
                            telephoneField.getText().trim(),
                            adresseField.getText().trim(),
                            specialite != null ? specialite.trim() : ""
                    );
                    service.creer(f);
                } else {
                    fournisseur.setNom(nomField.getText().trim());
                    fournisseur.setEmail(emailField.getText().trim());
                    fournisseur.setTelephone(telephoneField.getText().trim());
                    fournisseur.setAdresse(adresseField.getText().trim());
                    fournisseur.setSpecialite(specialite != null ? specialite.trim() : "");
                    service.modifier(fournisseur);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> { if (onSaved != null) onSaved.run(); });

        task.setOnFailed(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveBtnLabel.setText(fournisseur == null ? "Ajouter" : "Enregistrer");
            saveIcon.setIconLiteral("mdi2c-check:14");
            String msg = task.getException().getMessage();
            setError(nomField, errNom, "Erreur : " + (msg != null ? msg : "inconnue"));
        }));

        new Thread(task).start();
    }

    @FXML
    private void handleCancel() {
        if (onCancelled != null) onCancelled.run();
    }
}
