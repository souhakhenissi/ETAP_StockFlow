package org.example.controllers;

import com.sun.javafx.binding.LongConstant;
import org.example.models.Demande;
import org.example.models.LigneDemande;
import org.example.services.DemandeService;
import org.example.services.DemandePdfService;
import org.example.services.NotificationService;
import org.example.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.util.ArrayList;
import java.util.List;

/**
 * Remplace DemandeFormController.java
 * Utilise NotificationService pour email + notif in-app
 */
public class DemandeFormController {

    @FXML private Label    formTitle;
    @FXML private TextArea justificationField;
    @FXML private ComboBox<String> prioriteCombo;
    @FXML private VBox     articlesContainer;
    @FXML private Button   btnAjouterArticle, saveBtn;
    @FXML private Label    errPriorite, errArticles;

    private List<String[]> listMateriels = new ArrayList<>();
    private final List<Object[]> lignesArticles = new ArrayList<>();

    private Runnable onSaved;
    private Runnable onCancelled;

    private final DemandeService     demandeService  = new DemandeService();
    private final DemandePdfService  pdfService      = new DemandePdfService();
    private final NotificationService notifService   = new NotificationService();

    @FXML
    public void initialize() {
        prioriteCombo.setItems(FXCollections.observableArrayList(
                "normale", "haute", "urgente"));
        prioriteCombo.setValue("haute");

        Task<List<String[]>> task = new Task<>() {
            @Override protected List<String[]> call() throws Exception {
                return demandeService.findMateriels();
            }
        };
        task.setOnSucceeded(e -> {
            listMateriels = task.getValue();
            ajouterLigneArticle();
        });
        new Thread(task).start();
    }

    @FXML
    private void handleAjouterArticle() { ajouterLigneArticle(); }

    private void ajouterLigneArticle() {
        ComboBox<String> materielCombo = new ComboBox<>();
        materielCombo.setEditable(true);
        materielCombo.setPromptText("Materiel...");
        materielCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(materielCombo, Priority.ALWAYS);

        List<String> noms = listMateriels.stream()
                .map(a -> a[1] + " (" + a[2] + ")").toList();
        materielCombo.setItems(FXCollections.observableArrayList(noms));
        materielCombo.setStyle(
                "-fx-background-color:#f9fafb;-fx-border-color:#e2e8f0;"
                        + "-fx-border-radius:8;-fx-background-radius:8;"
                        + "-fx-pref-height:38px;-fx-font-size:13px;");

        // Spinner quantité
        Spinner<Integer> qteSpinner = new Spinner<>();
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1);
        qteSpinner.setValueFactory(valueFactory);
        qteSpinner.setEditable(true);
        qteSpinner.setPrefWidth(80);
        qteSpinner.setMaxWidth(80);

        // 1) Restreindre la saisie aux chiffres (via TextFormatter)
        qteSpinner.getEditor().setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) { // uniquement chiffres
                return change;
            }
            return null; // rejeter
        }));

        // 2) Valider et corriger la valeur à la perte de focus
        qteSpinner.getEditor().focusedProperty().addListener((obs, old, newFocus) -> {
            if (!newFocus) { // perte de focus
                String text = qteSpinner.getEditor().getText();
                if (text == null || text.trim().isEmpty()) {
                    valueFactory.setValue(1);
                } else {
                    try {
                        int val = Integer.parseInt(text.trim());
                        // Appliquer les bornes
                        if (val < 1)      valueFactory.setValue(1);
                        else if (val > 999) valueFactory.setValue(999);
                        else              valueFactory.setValue(val);
                    } catch (NumberFormatException e) {
                        valueFactory.setValue(1); // fallback
                    }
                }
            }
        });

        Button btnSuppr = new Button();
        btnSuppr.setGraphic(FontIcon.of(MaterialDesignC.CLOSE, 14,
                Color.web("#6b7280")));
        btnSuppr.setStyle("-fx-background-color:transparent;-fx-cursor:hand;"
                + "-fx-padding:6;-fx-border-color:transparent;");

        HBox ligne = new HBox(10, materielCombo, qteSpinner, btnSuppr);
        ligne.setAlignment(Pos.CENTER_LEFT);

        Object[] entree = {materielCombo, qteSpinner, ligne};
        lignesArticles.add(entree);
        articlesContainer.getChildren().add(ligne);

        btnSuppr.setOnAction(e -> {
            if (lignesArticles.size() <= 1) return;
            lignesArticles.remove(entree);
            articlesContainer.getChildren().remove(ligne);
        });
    }

    private boolean valider() {
        boolean ok = true;
        boolean auMoinsUn = false;
        for (Object[] l : lignesArticles) {
            ComboBox<String> cb = (ComboBox<String>) l[0];
            String val = cb.getValue();
            if (val == null || val.isBlank()) val = cb.getEditor().getText();
            if (val != null && !val.isBlank()) { auMoinsUn = true; break; }
        }
        if (!auMoinsUn) {
            errArticles.setText("Renseignez au moins un article.");
            errArticles.setVisible(true); errArticles.setManaged(true);
            ok = false;
        } else {
            errArticles.setVisible(false); errArticles.setManaged(false);
        }
        return ok;
    }

    @FXML
    private void handleSave() {
        if (!valider()) return;

        saveBtn.setDisable(true);
        saveBtn.setText("Envoi en cours...");

        var user = SessionManager.getInstance().getUtilisateurConnecte();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {

                // Construire la demande
                Demande d = new Demande();
                d.setNumeroDemande(demandeService.genererNumeroDemande());
                d.setIdIntervenant(user.getId());
                d.setNomIntervenant(user.getNomComplet());
                d.setEmailIntervenant(user.getEmail());
                d.setSiteIntervenant(user.getSiteAffecte());
                d.setJustification(justificationField.getText().trim());

                try {
                    d.setPriorite(Demande.Priorite.valueOf(
                            prioriteCombo.getValue()));
                } catch (Exception e) {
                    d.setPriorite(Demande.Priorite.normale);
                }

                // Construire les lignes articles
                List<LigneDemande> lignes = new ArrayList<>();
                for (Object[] entree : lignesArticles) {
                    ComboBox<String> cb = (ComboBox<String>) entree[0];
                    Spinner<Integer> qteSpinner = (Spinner<Integer>) entree[1];
                    String nomComplet  = cb.getValue();
                    if (nomComplet == null || nomComplet.isBlank())
                        nomComplet = cb.getEditor().getText().trim();
                    if (nomComplet == null || nomComplet.isBlank()) continue;

                    int qte = qteSpinner.getValue();

                    String fn = nomComplet;
                    int idMat = listMateriels.stream()
                            .filter(a -> (a[1] + " (" + a[2] + ")").equals(fn))
                            .map(a -> Integer.parseInt(a[0]))
                            .findFirst().orElse(0);

                    String nomPropre = nomComplet.contains("(")
                            ? nomComplet.substring(0, nomComplet.lastIndexOf("(")).trim()
                            : nomComplet;

                    lignes.add(new LigneDemande(nomPropre, idMat, qte));
                }
                d.setLignes(lignes);

                // Persister en DB
                demandeService.creer(d);

                // Générer le PDF
                byte[] pdfBytes = pdfService.genererPdfBytes(d);

                // Notifier tous les admins (email + notif in-app)
                notifService.notifierNouvelleDemande(d, pdfBytes);

                return null;
            }
        };

        task.setOnSucceeded(e -> { if (onSaved != null) onSaved.run(); });
        task.setOnFailed(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveBtn.setText("Envoyer la demande");
            errArticles.setText("Erreur : " + task.getException().getMessage());
            errArticles.setVisible(true); errArticles.setManaged(true);
        }));

        new Thread(task).start();
    }

    @FXML private void handleCancel() {
        if (onCancelled != null) onCancelled.run();
    }

    public void setOnSaved(Runnable r)     { this.onSaved = r; }
    public void setOnCancelled(Runnable r) { this.onCancelled = r; }
}
