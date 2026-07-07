package org.example.controllers;

import org.example.models.MouvementStock;
import org.example.services.StockService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

public class MouvementFormController {

    // ── Toggle ─────────────────────────────────────────────────────────────────
    @FXML private Label  formTitle;
    @FXML private Button toggleEntree, toggleSortie, saveBtn;

    // ── Formulaire Entrée ──────────────────────────────────────────────────────
    @FXML private VBox             formEntree;
    @FXML private ComboBox<String> entMatérielCombo, entFournisseurCombo;
    @FXML private TextField        entQuantite, entPrix;
    @FXML private TextArea         entObservation;
    @FXML private Label            entErrMateriel, entErrQuantite;

    // ── Formulaire Sortie ──────────────────────────────────────────────────────
    @FXML private VBox             formSortie;
    @FXML private ComboBox<String> sorDemandeCombo, sorMaterielCombo, sorSiteCombo;
    @FXML private TextField        sorQuantite;
    @FXML private TextArea         sorObservation;
    @FXML private Label            sorErrDemande, sorErrMateriel, sorErrQuantite, sorErrSite;

    // ── État ───────────────────────────────────────────────────────────────────
    private MouvementStock.Type typeCourant    = MouvementStock.Type.entree;
    private MouvementStock      mouvementExist; // null = création
    // Garde les anciennes valeurs pour annuler l'effet stock en cas de modification
    private int                 ancienIdMat    = 0;
    private int                 ancienneQte    = 0;
    private MouvementStock.Type ancienType     = MouvementStock.Type.entree;

    private Runnable            onSaved;
    private Runnable            onCancelled;

    private List<String[]>      listMateriels;    // [id, nom, ref]
    private List<String[]>      listFournisseurs; // [id, nom]

    private final StockService service = new StockService();

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        sorSiteCombo.setItems(FXCollections.observableArrayList(
                "Khereddine Pacha", "Mohamed V", "Ghazela"
        ));

        Task<Void> loadTask = new Task<>() {
            @Override protected Void call() throws Exception {
                listMateriels    = service.findMateriels();
                listFournisseurs = service.findFournisseurs();
                return null;
            }
        };
        loadTask.setOnSucceeded(e -> {
            List<String> nomsMat  = listMateriels.stream()
                    .map(a -> a[1] + " (" + a[2] + ")").toList();
            List<String> nomsFour = listFournisseurs.stream()
                    .map(a -> a[1]).toList();

            entMatérielCombo.setItems(FXCollections.observableArrayList(nomsMat));
            sorMaterielCombo.setItems(FXCollections.observableArrayList(nomsMat));
            entFournisseurCombo.setItems(FXCollections.observableArrayList(nomsFour));

            // Demandes non traitées
            Task<List<String>> demTask = new Task<>() {
                @Override protected List<String> call() throws Exception {
                    return service.findNumerosDemandes();
                }
            };
            demTask.setOnSucceeded(ev -> {
                List<String> dems = demTask.getValue();
                // En modification, ré-ajouter la demande courante si absente
                if (mouvementExist != null
                        && mouvementExist.getNumeroDemande() != null
                        && !dems.contains(mouvementExist.getNumeroDemande())) {
                    dems = new java.util.ArrayList<>(dems);
                    dems.add(0, mouvementExist.getNumeroDemande());
                }
                sorDemandeCombo.setItems(FXCollections.observableArrayList(dems));
                if (mouvementExist != null) preRemplir();
            });
            new Thread(demTask).start();
        });
        new Thread(loadTask).start();
    }

    // ── Toggle Entrée / Sortie ─────────────────────────────────────────────────
    @FXML
    public void switchToEntree() {
        typeCourant = MouvementStock.Type.entree;
        formEntree.setVisible(true);  formEntree.setManaged(true);
        formSortie.setVisible(false); formSortie.setManaged(false);
        toggleEntree.getStyleClass().setAll("tab-btn-active");
        toggleSortie.getStyleClass().setAll("tab-btn-inactive");
        formTitle.setText(mouvementExist == null
                ? "Nouvelle entree stock" : "Modifier entree");
    }

    @FXML
    public void switchToSortie() {
        typeCourant = MouvementStock.Type.sortie;
        formSortie.setVisible(true);  formSortie.setManaged(true);
        formEntree.setVisible(false); formEntree.setManaged(false);
        toggleSortie.getStyleClass().setAll("tab-btn-active");
        toggleEntree.getStyleClass().setAll("tab-btn-inactive");
        formTitle.setText(mouvementExist == null
                ? "Nouvelle sortie stock" : "Modifier sortie");
    }

    // ── Setter mouvement (modification) ───────────────────────────────────────
    public void setMouvement(MouvementStock m) {
        this.mouvementExist = m;
        if (m == null) return;
        // Sauvegarder les anciennes valeurs pour rollback stock
        this.ancienIdMat  = m.getIdMateriel();
        this.ancienneQte  = m.getQuantite();
        this.ancienType   = m.getType();
        // Afficher le bon onglet
        if (m.getType() == MouvementStock.Type.sortie) switchToSortie();
        else switchToEntree();
    }

    // Pré-remplissage après chargement async des combos
    private void preRemplir() {
        MouvementStock m = mouvementExist;
        if (m.getType() == MouvementStock.Type.entree) {
            String nomMat = trouverNomMateriel(m.getIdMateriel());
            if (nomMat != null) entMatérielCombo.setValue(nomMat);
            entQuantite.setText(String.valueOf(m.getQuantite()));
            entPrix.setText(String.format("%.3f", m.getPrixUnitaire()));
            if (m.getNomFournisseur() != null)
                entFournisseurCombo.setValue(m.getNomFournisseur());
            if (m.getObservation() != null)
                entObservation.setText(m.getObservation());
        } else {
            // Pré-remplir le numéro de demande
            if (m.getNumeroDemande() != null)
                sorDemandeCombo.setValue(m.getNumeroDemande());
            String nomMat = trouverNomMateriel(m.getIdMateriel());
            if (nomMat != null) sorMaterielCombo.setValue(nomMat);
            sorQuantite.setText(String.valueOf(m.getQuantite()));
            if (m.getSiteEtap() != null) sorSiteCombo.setValue(m.getSiteEtap());
            if (m.getObservation() != null)
                sorObservation.setText(m.getObservation());
        }
    }

    // ── Résolution id matériel / fournisseur ───────────────────────────────────
    private String trouverNomMateriel(int id) {
        if (listMateriels == null) return null;
        return listMateriels.stream()
                .filter(a -> Integer.parseInt(a[0]) == id)
                .map(a -> a[1] + " (" + a[2] + ")")
                .findFirst().orElse(null);
    }

    private int getIdMaterielParNom(String nomComplet) {
        if (listMateriels == null || nomComplet == null) return 0;
        return listMateriels.stream()
                .filter(a -> (a[1] + " (" + a[2] + ")").equals(nomComplet))
                .map(a -> Integer.parseInt(a[0]))
                .findFirst().orElse(0);
    }

    private int getIdFournisseurParNom(String nom) {
        if (listFournisseurs == null || nom == null) return 0;
        return listFournisseurs.stream()
                .filter(a -> a[1].equals(nom))
                .map(a -> Integer.parseInt(a[0]))
                .findFirst().orElse(0);
    }

    // ── Validation ─────────────────────────────────────────────────────────────
    private boolean validerEntree() {
        boolean ok = true;
        if (entMatérielCombo.getValue() == null) {
            setErr(entErrMateriel, "Materiel obligatoire."); ok = false;
        } else clearErr(entErrMateriel);

        try {
            int q = Integer.parseInt(entQuantite.getText().trim());
            if (q <= 0) throw new NumberFormatException();
            clearErr(entErrQuantite);
        } catch (NumberFormatException e) {
            setErr(entErrQuantite, "Quantite invalide (entier > 0)."); ok = false;
        }
        return ok;
    }

    private boolean validerSortie() {
        boolean ok = true;
        if (sorMaterielCombo.getValue() == null) {
            setErr(sorErrMateriel, "Materiel obligatoire."); ok = false;
        } else clearErr(sorErrMateriel);

        try {
            int q = Integer.parseInt(sorQuantite.getText().trim());
            if (q <= 0) throw new NumberFormatException();
            clearErr(sorErrQuantite);
        } catch (NumberFormatException e) {
            setErr(sorErrQuantite, "Quantite invalide (entier > 0)."); ok = false;
        }
        if (sorSiteCombo.getValue() == null) {
            setErr(sorErrSite, "Site ETAP obligatoire."); ok = false;
        } else clearErr(sorErrSite);
        return ok;
    }

    private void setErr(Label l, String msg) {
        l.setText(msg); l.setVisible(true); l.setManaged(true);
    }
    private void clearErr(Label l) {
        l.setVisible(false); l.setManaged(false);
    }

    // ── Sauvegarde ─────────────────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        boolean valid = typeCourant == MouvementStock.Type.entree
                ? validerEntree() : validerSortie();
        if (!valid) return;

        saveBtn.setDisable(true);
        saveBtn.setText("Enregistrement...");

        // Capturer les valeurs anciennes avant modification async
        final int      savedAncienIdMat = ancienIdMat;
        final int      savedAncienneQte = ancienneQte;
        final MouvementStock.Type savedAncienType = ancienType;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                MouvementStock m = mouvementExist != null
                        ? mouvementExist : new MouvementStock();

                m.setType(typeCourant);

                if (typeCourant == MouvementStock.Type.entree) {
                    m.setIdMateriel(getIdMaterielParNom(entMatérielCombo.getValue()));
                    m.setQuantite(Integer.parseInt(entQuantite.getText().trim()));
                    try { m.setPrixUnitaire(
                            Double.parseDouble(entPrix.getText().trim())); }
                    catch (NumberFormatException e) { m.setPrixUnitaire(0); }
                    m.setIdFournisseur(getIdFournisseurParNom(
                            entFournisseurCombo.getValue()));
                    m.setNomFournisseur(entFournisseurCombo.getValue());
                    m.setNumeroDemande(null);
                    m.setSiteEtap(null);
                    m.setObservation(entObservation.getText().trim());
                } else {
                    String dem = sorDemandeCombo.getValue();
                    if (dem == null || dem.isBlank())
                        dem = sorDemandeCombo.getEditor() != null
                                ? sorDemandeCombo.getEditor().getText().trim() : null;
                    m.setNumeroDemande(dem);
                    m.setIdMateriel(getIdMaterielParNom(sorMaterielCombo.getValue()));
                    m.setQuantite(Integer.parseInt(sorQuantite.getText().trim()));
                    m.setSiteEtap(sorSiteCombo.getValue());
                    m.setPrixUnitaire(0);
                    m.setIdFournisseur(0);
                    m.setNomFournisseur(null);
                    m.setObservation(sorObservation.getText().trim());
                }

                if (mouvementExist == null) {
                    // Création
                    m.setNumeroBon(service.genererNumeroBon(typeCourant));
                    service.creer(m);
                } else {
                    // Modification — passer les anciennes valeurs pour le rollback stock
                    service.modifier(m, savedAncienIdMat, savedAncienneQte, savedAncienType);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> { if (onSaved != null) onSaved.run(); });
        task.setOnFailed(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveBtn.setText("Enregistrer");
            String msg = task.getException().getMessage();
            setErr(entErrMateriel, "Erreur : " + (msg != null ? msg : "inconnue"));
        }));

        new Thread(task).start();
    }

    @FXML private void handleCancel() {
        if (onCancelled != null) onCancelled.run();
    }

    public void setOnSaved(Runnable r)     { this.onSaved = r; }
    public void setOnCancelled(Runnable r) { this.onCancelled = r; }
}
