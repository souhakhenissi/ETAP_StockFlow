package org.example.controllers;

import org.example.models.Materiel;
import org.example.services.MaterielService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class MaterielFormController {

    // ── Champs ─────────────────────────────────────────────────────────────────
    @FXML private Label    formTitle;
    @FXML private TextField nomField, referenceField, marqueField;
    @FXML private TextField modeleField, numeroSerieField;
    @FXML private TextField quantiteField, seuilField;
    @FXML private ComboBox<String> categorieCombo, etatCombo, fournisseurCombo;
    @FXML private DatePicker datePicker;

    // ── Erreurs ────────────────────────────────────────────────────────────────
    @FXML private Label errNom, errReference, errCategorie, errEtat, errQuantite;

    // ── Bouton save ────────────────────────────────────────────────────────────
    @FXML private Button   saveBtn;
    @FXML private Label    saveBtnLabel;
    @FXML private FontIcon saveIcon;

    private Materiel          materiel;
    private Runnable          onSaved;
    private Runnable          onCancelled;
    private final MaterielService service = new MaterielService();

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Catégories prédéfinies + saisie libre
        categorieCombo.setItems(FXCollections.observableArrayList(
                "Switch", "Routeur", "Pare-feu", "Access Point",
                "Serveur", "Stockage", "Disque dur", "SSD",
                "Câble fibre", "Câble RJ45", "Brassage",
                "Onduleur", "Téléphonie IP", "Imprimante",
                "Écran", "Ordinateur de bureau", "Ordinateur portable",
                "Consommable", "Autre"
        ));

        // États
        etatCombo.setItems(FXCollections.observableArrayList(
                "neuf", "en_service", "en_panne", "reforme"));

        // Valeurs par défaut
        etatCombo.setValue("neuf");
        quantiteField.setText("0");
        seuilField.setText("2");

        // Charger les fournisseurs depuis la DB
        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() throws Exception {
                return service.findNomsFournisseurs();
            }
        };
        task.setOnSucceeded(e -> {
            fournisseurCombo.setItems(FXCollections.observableArrayList(task.getValue()));
        });
        new Thread(task).start();

        // Validation à la volée
        nomField.focusedProperty().addListener((o, ov, nv)       -> { if (!nv) validerNom(); });
        referenceField.focusedProperty().addListener((o, ov, nv) -> { if (!nv) validerReference(); });
        quantiteField.focusedProperty().addListener((o, ov, nv)  -> { if (!nv) validerQuantite(); });
    }

    public void setMateriel(Materiel m) {
        this.materiel = m;
        if (m == null) {
            formTitle.setText("Nouveau matériel");
            saveBtnLabel.setText("Ajouter");
        } else {
            formTitle.setText("Modifier — " + m.getNom());
            nomField.setText(m.getNom() != null ? m.getNom() : "");
            referenceField.setText(m.getReference() != null ? m.getReference() : "");
            marqueField.setText(m.getMarque() != null ? m.getMarque() : "");
            modeleField.setText(m.getModele() != null ? m.getModele() : "");
            numeroSerieField.setText(m.getNumeroSerie() != null ? m.getNumeroSerie() : "");
            quantiteField.setText(String.valueOf(m.getQuantiteStock()));
            seuilField.setText(String.valueOf(m.getSeuilAlerte()));
            if (m.getDateAcquisition() != null) datePicker.setValue(m.getDateAcquisition());
            if (m.getEtat() != null) etatCombo.setValue(m.getEtat().name());

            // Catégorie : liste ou saisie libre
            if (m.getCategorie() != null && !m.getCategorie().isBlank()) {
                if (categorieCombo.getItems().contains(m.getCategorie()))
                    categorieCombo.setValue(m.getCategorie());
                else
                    categorieCombo.getEditor().setText(m.getCategorie());
            }

            // Fournisseur (chargé en async dans initialize — attendre si nécessaire)
            if (m.getNomFournisseur() != null && !m.getNomFournisseur().isBlank()) {
                // Post-initialisation : setter déclenché après le chargement des fournisseurs
                fournisseurCombo.getSelectionModel().selectedItemProperty()
                        .addListener((o, ov, nv) -> {}); // listener vide pour forcer refresh
                Platform.runLater(() -> fournisseurCombo.setValue(m.getNomFournisseur()));
            }
            saveBtnLabel.setText("Enregistrer");
        }
    }

    public void setOnSaved(Runnable r)     { this.onSaved = r; }
    public void setOnCancelled(Runnable r) { this.onCancelled = r; }

    // ── Validation ─────────────────────────────────────────────────────────────
    private boolean validerNom() {
        if (nomField.getText().trim().isEmpty()) {
            setErr(nomField, errNom, "Le nom est obligatoire."); return false;
        }
        clearErr(nomField, errNom); return true;
    }

    private boolean validerReference() {
        if (referenceField.getText().trim().isEmpty()) {
            setErr(referenceField, errReference, "La référence est obligatoire."); return false;
        }
        clearErr(referenceField, errReference); return true;
    }

    private boolean validerCategorie() {
        String val = getCategorie();
        if (val == null || val.isBlank()) {
            setErr(categorieCombo, errCategorie, "La catégorie est obligatoire."); return false;
        }
        clearErr(categorieCombo, errCategorie); return true;
    }

    private boolean validerQuantite() {
        try {
            int q = Integer.parseInt(quantiteField.getText().trim());
            if (q < 0) throw new NumberFormatException();
            clearErr(quantiteField, errQuantite); return true;
        } catch (NumberFormatException e) {
            setErr(quantiteField, errQuantite, "Quantité invalide (entier ≥ 0)."); return false;
        }
    }

    private String getCategorie() {
        String val = categorieCombo.getValue();
        if (val == null || val.isBlank()) val = categorieCombo.getEditor().getText();
        return val != null ? val.trim() : null;
    }

    private void setErr(Control f, Label lbl, String msg) {
        f.getStyleClass().remove("error"); f.getStyleClass().add("error");
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private void clearErr(Control f, Label lbl) {
        f.getStyleClass().remove("error");
        lbl.setVisible(false); lbl.setManaged(false);
    }

    // ── Sauvegarde ─────────────────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        boolean valid = validerNom() & validerReference()
                & validerCategorie() & validerQuantite();
        if (!valid) return;

        saveBtn.setDisable(true);
        saveBtnLabel.setText("Enregistrement…");
        saveIcon.setIconLiteral("mdi2l-loading:14");

        // Résoudre l'id du fournisseur
        String nomFourn = fournisseurCombo.getValue();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                Materiel m = materiel != null ? materiel : new Materiel();

                m.setNom(nomField.getText().trim());
                m.setReference(referenceField.getText().trim());
                m.setCategorie(getCategorie());
                m.setMarque(marqueField.getText().trim());
                m.setModele(modeleField.getText().trim());
                m.setNumeroSerie(numeroSerieField.getText().trim());
                m.setQuantiteStock(Integer.parseInt(quantiteField.getText().trim()));

                try { m.setSeuilAlerte(Integer.parseInt(seuilField.getText().trim())); }
                catch (NumberFormatException e) { m.setSeuilAlerte(2); }

                m.setDateAcquisition(datePicker.getValue());

                try { m.setEtat(Materiel.Etat.valueOf(etatCombo.getValue())); }
                catch (Exception e) { m.setEtat(Materiel.Etat.neuf); }

                if (nomFourn != null && !nomFourn.isBlank()) {
                    int idF = service.getIdFournisseurByNom(nomFourn);
                    m.setIdFournisseur(idF);
                }

                if (materiel == null) service.creer(m);
                else                  service.modifier(m);
                return null;
            }
        };

        task.setOnSucceeded(e -> { if (onSaved != null) onSaved.run(); });
        task.setOnFailed(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveBtnLabel.setText(materiel == null ? "Ajouter" : "Enregistrer");
            saveIcon.setIconLiteral("mdi2c-check:14");
            String msg = task.getException().getMessage();
            if (msg != null && msg.contains("Duplicate"))
                setErr(referenceField, errReference, "Cette référence existe déjà.");
            else
                setErr(nomField, errNom, "Erreur : " + (msg != null ? msg : "inconnue"));
        }));

        new Thread(task).start();
    }

    @FXML
    private void handleCancel() { if (onCancelled != null) onCancelled.run(); }
}
