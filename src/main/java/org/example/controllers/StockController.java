package org.example.controllers;

import org.example.models.MouvementStock;
import org.example.services.StockPdfService;
import org.example.services.StockService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StockController {

    // ── FXML ───────────────────────────────────────────────────────────────────
    @FXML private VBox     alertesBox;
    @FXML private FlowPane alertesCardsPane;
    @FXML private Button   tabEntrees, tabSorties;

    @FXML private TableView<MouvementStock> tableEntrees, tableSorties;

    // Colonnes entrées
    @FXML private TableColumn<MouvementStock, String>
            colEntBon, colEntDate, colEntMateriel, colEntFournisseur,
            colEntQte, colEntPrix, colEntMontant, colEntObs;
    @FXML private TableColumn<MouvementStock, Void> colEntActions;

    // Colonnes sorties
    @FXML private TableColumn<MouvementStock, String>
            colSorBon, colSorDate, colSorMateriel, colSorDemande,
            colSorSite, colSorQte, colSorObs;
    @FXML private TableColumn<MouvementStock, Void> colSorActions;

    // ── Données ────────────────────────────────────────────────────────────────
    private final ObservableList<MouvementStock> dataEntrees = FXCollections.observableArrayList();
    private final ObservableList<MouvementStock> dataSorties = FXCollections.observableArrayList();
    private final StockService    service    = new StockService();
    private final StockPdfService pdfService = new StockPdfService();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurerTableEntrees();
        configurerTableSorties();
        chargerTout();
    }

    // ── Table Entrées ──────────────────────────────────────────────────────────
    private void configurerTableEntrees() {
        // N° Bon
        colEntBon.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNumeroBon()));
        colEntBon.setCellFactory(col -> celluleBon());

        // Date
        colEntDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateCreation() != null
                        ? c.getValue().getDateCreation().format(FMT) : "—"));

        // Matériel (nom + référence)
        colEntMateriel.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNomMateriel()));
        colEntMateriel.setCellFactory(col -> celluleMateriel());

        // Fournisseur
        colEntFournisseur.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNomFournisseur() != null
                        ? c.getValue().getNomFournisseur() : "—"));
        colEntFournisseur.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                l.setStyle("-fx-text-fill:"
                        + (item.equals("—") ? "#d1d5db" : "#0154a6")
                        + ";-fx-font-size:12.5px;");
                setGraphic(l); setText(null);
            }
        });

        // Quantité
        colEntQte.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getQuantite())));
        colEntQte.setCellFactory(col -> celluleBold("#111827"));

        // Prix unitaire
        colEntPrix.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.format("%.3f", c.getValue().getPrixUnitaire())));

        // Montant total
        colEntMontant.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.format("%.3f", c.getValue().getMontantTotal())));
        colEntMontant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item + " TND");
                l.setStyle("-fx-font-weight:bold;-fx-text-fill:#16a34a;");
                setGraphic(l); setText(null);
            }
        });

        // Observation
        colEntObs.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getObservation() != null
                        ? c.getValue().getObservation() : ""));

        // Actions
        colEntActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                MouvementStock m = (MouvementStock) getTableRow().getItem();
                setGraphic(creerBoutonsActions(m));
                setText(null);
            }
        });

        tableEntrees.setItems(dataEntrees);
        tableEntrees.setPlaceholder(new Label("Aucune entree enregistree."));
        tableEntrees.setFixedCellSize(54);
    }

    // ── Table Sorties ──────────────────────────────────────────────────────────
    private void configurerTableSorties() {
        // N° Bon
        colSorBon.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNumeroBon()));
        colSorBon.setCellFactory(col -> celluleBon());

        // Date
        colSorDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateCreation() != null
                        ? c.getValue().getDateCreation().format(FMT) : "—"));

        // Matériel
        colSorMateriel.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNomMateriel()));
        colSorMateriel.setCellFactory(col -> celluleMateriel());

        // N° Demande
        colSorDemande.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNumeroDemande() != null
                        ? c.getValue().getNumeroDemande() : "—"));
        colSorDemande.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals("—")) {
                    setText(item); setGraphic(null); return;
                }
                Label l = new Label(item);
                l.setStyle("-fx-font-family:monospace;-fx-font-size:11px;"
                        + "-fx-text-fill:#0154a6;-fx-background-color:#e8f0fb;"
                        + "-fx-background-radius:4;-fx-padding:2 6;");
                setGraphic(l); setText(null);
            }
        });

        // Site
        colSorSite.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSiteEtap() != null
                        ? c.getValue().getSiteEtap() : "—"));

        // Quantité
        colSorQte.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getQuantite())));
        colSorQte.setCellFactory(col -> celluleBold("#111827"));

        // Observation
        colSorObs.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getObservation() != null
                        ? c.getValue().getObservation() : ""));

        // Actions
        colSorActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                MouvementStock m = (MouvementStock) getTableRow().getItem();
                setGraphic(creerBoutonsActions(m));
                setText(null);
            }
        });

        tableSorties.setItems(dataSorties);
        tableSorties.setPlaceholder(new Label("Aucune sortie enregistree."));
        tableSorties.setFixedCellSize(54);
    }

    // ── Cellules réutilisables ─────────────────────────────────────────────────
    private <T> TableCell<T, String> celluleBon() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                l.setStyle("-fx-font-family:monospace;-fx-font-size:11px;"
                        + "-fx-text-fill:#374151;-fx-background-color:#f3f4f6;"
                        + "-fx-background-radius:4;-fx-padding:2 6;");
                setGraphic(l); setText(null);
            }
        };
    }

    private <T> TableCell<T, String> celluleMateriel() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                MouvementStock m = (MouvementStock) getTableRow().getItem();
                Label nom = new Label(m.getNomMateriel() != null
                        ? m.getNomMateriel() : "—");
                nom.setStyle("-fx-font-weight:bold;-fx-font-size:12.5px;"
                        + "-fx-text-fill:#111827;");
                Label ref = new Label(m.getReferenceMateriel() != null
                        ? m.getReferenceMateriel() : "");
                ref.setStyle("-fx-font-size:11px;-fx-text-fill:#9ca3af;");
                VBox box = new VBox(1, nom, ref);
                setGraphic(box); setText(null);
            }
        };
    }

    private <T> TableCell<T, String> celluleBold(String color) {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                l.setStyle("-fx-font-weight:bold;-fx-text-fill:" + color + ";");
                setGraphic(l); setText(null);
            }
        };
    }

    // ── Boutons d'actions (PDF + Modifier + Supprimer) ─────────────────────────
    private HBox creerBoutonsActions(MouvementStock m) {
        Button btnPdf = new Button();
        btnPdf.setGraphic(FontIcon.of(
                MaterialDesignF.FILE_PDF_BOX, 14, Color.web("#dc2626")));
        btnPdf.getStyleClass().add("btn-table-danger");
        btnPdf.setTooltip(new Tooltip("Telecharger PDF"));
        btnPdf.setOnAction(e -> telechargerPdf(m));

        Button btnEdit = new Button();
        btnEdit.setGraphic(FontIcon.of(
                MaterialDesignP.PENCIL_OUTLINE, 14, Color.web("#1d4ed8")));
        btnEdit.getStyleClass().add("btn-table-edit");
        btnEdit.setTooltip(new Tooltip("Modifier"));
        btnEdit.setOnAction(e -> ouvrirFormulaire(m));

        Button btnDel = new Button();
        btnDel.setGraphic(FontIcon.of(
                MaterialDesignD.DELETE_OUTLINE, 14, Color.web("#991b1b")));
        btnDel.getStyleClass().add("btn-table-danger");
        btnDel.setTooltip(new Tooltip("Supprimer"));
        btnDel.setOnAction(e -> confirmerSuppression(m));

        HBox box = new HBox(5, btnPdf, btnEdit, btnDel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ── Chargement ─────────────────────────────────────────────────────────────
    private void chargerTout() {
        runAsync(service::findEntrees,
                list -> { dataEntrees.setAll(list); return null; });
        runAsync(service::findSorties,
                list -> { dataSorties.setAll(list); return null; });
        runAsync(service::findAlertesStockBas, this::afficherAlertes);
    }

    private Void afficherAlertes(List<String[]> alertes) {
        alertesCardsPane.getChildren().clear();
        if (alertes.isEmpty()) {
            alertesBox.setVisible(false); alertesBox.setManaged(false);
        } else {
            alertesBox.setVisible(true); alertesBox.setManaged(true);
            for (String[] a : alertes) {
                VBox card = new VBox(4);
                card.setStyle("-fx-background-color:white;"
                        + "-fx-background-radius:10;-fx-padding:10 14;"
                        + "-fx-border-color:#e2e8f0;-fx-border-radius:10;"
                        + "-fx-border-width:1;-fx-min-width:200px;"
                        + "-fx-max-width:240px;");

                Label nom = new Label(a[0]);
                nom.setStyle("-fx-font-weight:bold;-fx-font-size:12.5px;"
                        + "-fx-text-fill:#111827;");
                Label ref = new Label(a[1]);
                ref.setStyle("-fx-font-size:11px;-fx-text-fill:#9ca3af;");

                Label qte = new Label(a[2] + " / seuil " + a[3]);
                qte.setStyle("-fx-font-size:11px;-fx-font-weight:bold;"
                        + "-fx-text-fill:#d97706;-fx-background-color:#fef3c7;"
                        + "-fx-background-radius:20;-fx-padding:2 8;");

                HBox bottom = new HBox(qte);
                bottom.setAlignment(Pos.CENTER_RIGHT);
                card.getChildren().addAll(nom, ref, bottom);
                alertesCardsPane.getChildren().add(card);
            }
        }
        return null;
    }

    // ── Onglets ────────────────────────────────────────────────────────────────
    @FXML private void switchToEntrees() {
        tableEntrees.setVisible(true);  tableEntrees.setManaged(true);
        tableSorties.setVisible(false); tableSorties.setManaged(false);
        tabEntrees.getStyleClass().setAll("tab-btn-active");
        tabSorties.getStyleClass().setAll("tab-btn-inactive");
    }

    @FXML private void switchToSorties() {
        tableSorties.setVisible(true);  tableSorties.setManaged(true);
        tableEntrees.setVisible(false); tableEntrees.setManaged(false);
        tabSorties.getStyleClass().setAll("tab-btn-active");
        tabEntrees.getStyleClass().setAll("tab-btn-inactive");
    }

    // ── Formulaire ─────────────────────────────────────────────────────────────
    @FXML private void handleNouveauMouvement() { ouvrirFormulaire(null); }

    private void ouvrirFormulaire(MouvementStock m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/views/admin/mouvement_form.fxml"));
            StackPane overlay = creerOverlay();
            Node form = loader.load();
            MouvementFormController ctrl = loader.getController();
            ctrl.setMouvement(m);
            ctrl.setOnSaved(() -> {
                fermerOverlay(overlay);
                chargerTout();
                afficherToast(
                        m == null ? "Mouvement enregistre"
                                : "Mouvement modifie",
                        m == null ? "Le bon a ete cree avec succes."
                                : "Les informations ont ete mises a jour.",
                        true
                );
            });
            ctrl.setOnCancelled(() -> fermerOverlay(overlay));
            StackPane.setAlignment(form, Pos.CENTER);
            overlay.getChildren().add(form);
            afficherOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            afficherToast("Erreur", "Impossible d'ouvrir le formulaire.", false);
        }
    }

    // ── PDF ────────────────────────────────────────────────────────────────────
    private void telechargerPdf(MouvementStock m) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le bon");
        fc.setInitialFileName(m.getNumeroBon() + ".pdf");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        // Utiliser la fenêtre de la table visible
        var window = tableEntrees.isVisible()
                ? tableEntrees.getScene().getWindow()
                : tableSorties.getScene().getWindow();

        File dest = fc.showSaveDialog(window);
        if (dest == null) return;

        runAsync(() -> {
            if (m.getType() == MouvementStock.Type.entree)
                return pdfService.genererPdfEntree(m, dest);
            else
                return pdfService.genererPdfSortie(m, dest);
        }, file -> {
            afficherToast("PDF genere", dest.getName(), true);
            return null;
        });
    }

    // ── Confirm suppression ────────────────────────────────────────────────────
    private void confirmerSuppression(MouvementStock m) {
        StackPane overlay = creerOverlay();

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(340);
        card.setMaxHeight(300);
        card.setStyle("-fx-background-color:white;-fx-background-radius:14;"
                + "-fx-padding:28 28 24 28;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.16),24,0,0,6);");

        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(52, 52); iconCircle.setMaxSize(52, 52);
        iconCircle.setStyle(
                "-fx-background-radius:50;-fx-background-color:#fee2e2;");
        iconCircle.getChildren().add(
                FontIcon.of(MaterialDesignD.DELETE_OUTLINE, 24, Color.web("#dc2626")));

        Label titre = new Label("Supprimer ce mouvement");
        titre.setStyle("-fx-font-size:15px;-fx-font-weight:bold;"
                + "-fx-text-fill:#111827;");

        Label corps = new Label(
                "Voulez-vous supprimer le bon " + m.getNumeroBon()
                        + " ?\nLa quantite en stock sera automatiquement ajustee.");
        corps.setStyle("-fx-font-size:12.5px;-fx-text-fill:#6b7280;"
                + "-fx-text-alignment:center;");
        corps.setWrapText(true);
        corps.setMaxWidth(280);
        corps.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button btnOui = new Button("Supprimer");
        btnOui.getStyleClass().add("btn-table-danger");
        btnOui.setStyle("-fx-padding:8 24;-fx-font-size:13px;");

        Button btnNon = new Button("Annuler");
        btnNon.getStyleClass().add("btn-secondary");
        btnNon.setStyle("-fx-padding:8 24;-fx-font-size:13px;");

        HBox btns = new HBox(10, btnNon, btnOui);
        btns.setAlignment(Pos.CENTER);

        card.getChildren().addAll(iconCircle, titre, corps, btns);
        overlay.getChildren().add(card);

        btnNon.setOnAction(e -> fermerOverlay(overlay));
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) fermerOverlay(overlay);
        });
        btnOui.setOnAction(e -> {
            fermerOverlay(overlay);
            runAsync(() -> { service.supprimer(m); return null; }, v -> {
                chargerTout();
                afficherToast("Mouvement supprime", m.getNumeroBon(), false);
                return null;
            });
        });

        afficherOverlay(overlay);
    }

    // ── Toast (style FournisseursController) ──────────────────────────────────
    private void afficherToast(String titre, String sousTitre, boolean success) {
        StackPane contentPane = getContentPane();
        if (contentPane == null) return;

        String bgColor   = success ? "#16a34a" : "#dc2626";
        var    toastIkon = success
                ? MaterialDesignC.CHECK_CIRCLE_OUTLINE
                : MaterialDesignI.INFORMATION_OUTLINE;

        VBox toast = new VBox(4);
        toast.setStyle("-fx-background-color:" + bgColor + ";"
                + "-fx-background-radius:8;-fx-padding:12 16 12 16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),12,0,0,4);");
        toast.setMaxWidth(300); toast.setMinWidth(240);
        toast.setMaxHeight(40);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = FontIcon.of(toastIkon, 18, Color.WHITE);

        Label titleLbl = new Label(titre);
        titleLbl.setStyle(
                "-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(FontIcon.of(MaterialDesignC.CLOSE, 14, Color.WHITE));
        closeBtn.setStyle("-fx-background-color:transparent;-fx-padding:0;"
                + "-fx-cursor:hand;-fx-border-color:transparent;");
        row.getChildren().addAll(icon, titleLbl, spacer, closeBtn);

        Label subLbl = new Label(sousTitre);
        subLbl.setStyle("-fx-font-size:12px;"
                + "-fx-text-fill:rgba(255,255,255,0.85);-fx-padding:0 0 0 28;");
        subLbl.setWrapText(true);

        toast.getChildren().addAll(row, subLbl);
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new Insets(16, 16, 0, 0));
        contentPane.getChildren().add(toast);

        TranslateTransition si = new TranslateTransition(Duration.millis(250), toast);
        si.setFromX(320); si.setToX(0);
        FadeTransition fi = new FadeTransition(Duration.millis(200), toast);
        fi.setFromValue(0); fi.setToValue(1);
        si.play(); fi.play();

        Runnable fermer = () -> {
            FadeTransition fo = new FadeTransition(Duration.millis(250), toast);
            TranslateTransition so = new TranslateTransition(Duration.millis(250), toast);
            fo.setFromValue(1); fo.setToValue(0);
            so.setFromX(0); so.setToX(320);
            fo.setOnFinished(ev -> contentPane.getChildren().remove(toast));
            fo.play(); so.play();
        };
        closeBtn.setOnAction(e -> fermer.run());
        PauseTransition pause = new PauseTransition(Duration.seconds(3.5));
        pause.setOnFinished(e -> fermer.run());
        pause.play();
    }

    // ── Overlay helpers ────────────────────────────────────────────────────────
    private StackPane creerOverlay() {
        StackPane o = new StackPane();
        o.setStyle("-fx-background-color:rgba(0,0,0,0.45);");
        return o;
    }

    private void afficherOverlay(StackPane overlay) {
        StackPane cp = getContentPane();
        if (cp == null) return;
        cp.getChildren().add(overlay);
        FadeTransition ft = new FadeTransition(Duration.millis(180), overlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void fermerOverlay(StackPane overlay) {
        FadeTransition ft = new FadeTransition(Duration.millis(150), overlay);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            if (overlay.getParent() instanceof Pane p)
                p.getChildren().remove(overlay);
        });
        ft.play();
    }

    private StackPane getContentPane() {
        try {
            return (StackPane) tableEntrees.getScene().lookup("#contentPane");
        } catch (Exception e) { return null; }
    }

    // ── Async ──────────────────────────────────────────────────────────────────
    private <T> void runAsync(SqlSupplier<T> s, javafx.util.Callback<T, Void> cb) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return s.get(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> cb.call(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() ->
                afficherToast("Erreur", task.getException().getMessage(), false)));
        new Thread(task).start();
    }

    @FunctionalInterface interface SqlSupplier<T> { T get() throws Exception; }
}
