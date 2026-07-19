package org.example.controllers;

import org.example.models.Demande;
import org.example.services.DemandeService;
import org.example.services.DemandePdfService;
import org.example.services.NotificationService;
import org.example.utils.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import java.io.File;
import java.time.format.DateTimeFormatter;

/**
 * Remplace AdminDemandesController.java
 * Utilise NotificationService pour toutes les actions admin
 */
public class AdminDemandesController {

    @FXML private Label cardTotal, cardAttente, cardApprouvees,
            cardRejetees, cardLivrees;

    @FXML private TableView<Demande>           tableDemandes;
    @FXML private TableColumn<Demande, String> colNumero, colDate, colIntervenant,
            colArticles, colMotif,
            colPriorite, colStatut;
    @FXML private TableColumn<Demande, Void>   colActions;

    private final ObservableList<Demande> data     = FXCollections.observableArrayList();
    private final DemandeService   service          = new DemandeService();
    private final DemandePdfService pdfService      = new DemandePdfService();
    private final NotificationService notifService  = new NotificationService();

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        configurerTable();
        chargerDonnees();
    }

    private void configurerTable() {
        colNumero.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNumeroDemande()));
        colNumero.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                l.setStyle("-fx-font-family:monospace;-fx-font-size:11.5px;"
                        + "-fx-text-fill:#0154a6;");
                setGraphic(l); setText(null);
            }
        });

        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateCreation() != null
                        ? c.getValue().getDateCreation().format(FMT) : "—"));

        colIntervenant.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNomIntervenant() != null
                        ? c.getValue().getNomIntervenant() : "—"));
        colIntervenant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Demande d = (Demande) getTableRow().getItem();
                Label nom = new Label(d.getNomIntervenant() != null
                        ? d.getNomIntervenant() : "—");
                nom.setStyle("-fx-font-weight:bold;-fx-font-size:12px;"
                        + "-fx-text-fill:#111827;");
                Label site = new Label(d.getSiteIntervenant() != null
                        ? d.getSiteIntervenant() : "");
                site.setStyle("-fx-font-size:11px;-fx-text-fill:#9ca3af;");
                VBox box = new VBox(1, nom, site);
                setGraphic(box); setText(null);
            }
        });

        colArticles.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getArticlesResume()));
        colArticles.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                l.setStyle("-fx-font-size:12px;-fx-text-fill:#111827;");
                l.setWrapText(true);
                setGraphic(l); setText(null);
            }
        });

        colMotif.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getJustification() != null
                        ? c.getValue().getJustification() : ""));
        colMotif.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null); return;
                }
                Label l = new Label(item);
                l.setStyle("-fx-font-size:11.5px;-fx-text-fill:#6b7280;");
                l.setWrapText(true);
                setGraphic(l); setText(null);
            }
        });

        colPriorite.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPrioriteLabel()));
        colPriorite.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Demande d = (Demande) getTableRow().getItem();
                Label badge = new Label(d.getPrioriteLabel());
                badge.getStyleClass().addAll("badge", d.getPrioriteStyle());
                setGraphic(badge); setText(null);
            }
        });

        colStatut.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatutLabel()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Demande d = (Demande) getTableRow().getItem();
                Label badge = new Label(d.getStatutLabel());
                badge.getStyleClass().addAll("badge", d.getStatutStyle());
                setGraphic(badge); setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Demande d = (Demande) getTableRow().getItem();
                setGraphic(creerBoutonsActions(d));
                setText(null);
            }
        });

        tableDemandes.setItems(data);
        tableDemandes.setPlaceholder(new Label("Aucune demande."));
        tableDemandes.setFixedCellSize(58);
    }

    private HBox creerBoutonsActions(Demande d) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        // PDF toujours visible
        Button btnPdf = new Button();
        btnPdf.setGraphic(FontIcon.of(
                MaterialDesignF.FILE_PDF_BOX, 14, Color.web("#dc2626")));
        btnPdf.getStyleClass().add("btn-table-danger");
        btnPdf.setTooltip(new Tooltip("Telecharger PDF"));
        btnPdf.setOnAction(e -> telechargerPdf(d));
        box.getChildren().add(btnPdf);

        switch (d.getStatut()) {
            case en_attente -> {
                Button btnAccepter = new Button();
                btnAccepter.setGraphic(FontIcon.of(
                        MaterialDesignC.CHECK, 13, Color.web("#166534")));
                btnAccepter.getStyleClass().add("btn-table-unblock");
                btnAccepter.setOnAction(e -> approuver(d));

                Button btnRejeter = new Button();
                btnRejeter.setGraphic(FontIcon.of(
                        MaterialDesignC.CLOSE, 13, Color.web("#991b1b")));
                btnRejeter.getStyleClass().add("btn-table-danger");
                btnRejeter.setOnAction(e -> ouvrirFormulaireRejet(d));

                box.getChildren().addAll(btnAccepter, btnRejeter);
            }
            case approuvee -> {
                Button btnLivree = new Button("Marquer livree");
                btnLivree.setGraphic(FontIcon.of(
                        MaterialDesignP.PACKAGE_VARIANT_CLOSED, 13,
                        Color.web("#1d4ed8")));
                btnLivree.getStyleClass().add("btn-table-edit");
                btnLivree.setOnAction(e -> marquerLivree(d));
                box.getChildren().add(btnLivree);
            }
            default -> {} // rejetee, livree, annulee → PDF seulement
        }

        return box;
    }

    // ── Chargement ─────────────────────────────────────────────────────────────
    private void chargerDonnees() {
        runAsync(service::findAll, list -> {
            data.setAll(list);
            mettreAJourStats();
            return null;
        });
    }

    private void mettreAJourStats() {
        runAsync(() -> new int[]{
                service.countTotal(),    service.countEnAttente(),
                service.countApprouvees(), service.countRejetees(),
                service.countLivrees()
        }, s -> {
            cardTotal.setText(String.valueOf(s[0]));
            cardAttente.setText(String.valueOf(s[1]));
            cardApprouvees.setText(String.valueOf(s[2]));
            cardRejetees.setText(String.valueOf(s[3]));
            cardLivrees.setText(String.valueOf(s[4]));
            return null;
        });
    }

    // ── Actions admin ──────────────────────────────────────────────────────────
    private void approuver(Demande d) {
        int idAdmin = SessionManager.getInstance().getUtilisateurConnecte().getId();
        runAsync(() -> {
            service.approuver(d.getId(), idAdmin);
            Demande dComplete = service.findById(d.getId());
            if (dComplete != null) notifService.notifierDemandeApprouvee(dComplete);
            return null;
        }, v -> {
            chargerDonnees();
            afficherToast("Demande approuvee",
                    d.getNumeroDemande() + " — L'intervenant a ete notifie.", true);
            return null;
        });
    }

    private void marquerLivree(Demande d) {
        int idAdmin = SessionManager.getInstance().getUtilisateurConnecte().getId();
        runAsync(() -> {
            service.marquerLivree(d.getId(), idAdmin);
            Demande dComplete = service.findById(d.getId());
            if (dComplete != null) notifService.notifierDemandeLivree(dComplete);
            return null;
        }, v -> {
            chargerDonnees();
            afficherToast("Demande livree",
                    d.getNumeroDemande() + " — L'intervenant a ete notifie.", true);
            return null;
        });
    }

    private void ouvrirFormulaireRejet(Demande d) {
        StackPane overlay = creerOverlay();

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setMaxHeight(300);
        card.setStyle("-fx-background-color:white;-fx-background-radius:14;"
                + "-fx-padding:28 28 24 28;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.16),24,0,0,6);");

        StackPane ic = new StackPane();
        ic.setMinSize(52, 52); ic.setMaxSize(52, 52);
        ic.setStyle("-fx-background-radius:50;-fx-background-color:#fee2e2;");
        ic.getChildren().add(FontIcon.of(
                MaterialDesignC.CLOSE_CIRCLE_OUTLINE, 24, Color.web("#dc2626")));

        Label titre = new Label("Rejeter la demande");
        titre.setStyle("-fx-font-size:15px;-fx-font-weight:bold;"
                + "-fx-text-fill:#111827;");

        Label sousTitre = new Label("Motif du rejet (optionnel) :");
        sousTitre.setStyle("-fx-font-size:12.5px;-fx-text-fill:#6b7280;");

        TextArea motifArea = new TextArea();
        motifArea.setPromptText("Justification du rejet...");
        motifArea.setPrefRowCount(3);
        motifArea.setStyle(
                "-fx-background-color:#f9fafb;-fx-border-color:#e2e8f0;"
                        + "-fx-border-radius:8;-fx-background-radius:8;"
                        + "-fx-font-size:13px;-fx-pref-height:80px;");

        Button btnEnvoyer = new Button("Envoyer le rejet");
        btnEnvoyer.getStyleClass().add("btn-table-danger");
        btnEnvoyer.setStyle("-fx-padding:8 20;-fx-font-size:13px;");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.getStyleClass().add("btn-secondary");
        btnAnnuler.setStyle("-fx-padding:8 20;-fx-font-size:13px;");

        HBox btns = new HBox(10, btnAnnuler, btnEnvoyer);
        btns.setAlignment(Pos.CENTER);

        card.getChildren().addAll(ic, titre, sousTitre, motifArea, btns);
        overlay.getChildren().add(card);

        btnAnnuler.setOnAction(e -> fermerOverlay(overlay));
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) fermerOverlay(overlay);
        });

        btnEnvoyer.setOnAction(e -> {
            String motif = motifArea.getText().trim();
            int idAdmin = SessionManager.getInstance()
                    .getUtilisateurConnecte().getId();
            fermerOverlay(overlay);
            runAsync(() -> {
                service.rejeter(d.getId(), idAdmin,
                        motif.isBlank() ? null : motif);
                Demande dComplete = service.findById(d.getId());
                if (dComplete != null)
                    notifService.notifierDemandeRejetee(dComplete,
                            motif.isBlank() ? null : motif);
                return null;
            }, v -> {
                chargerDonnees();
                afficherToast("Demande rejetee",
                        d.getNumeroDemande() + " — L'intervenant a ete notifie.", false);
                return null;
            });
        });

        afficherOverlay(overlay);
    }

    private void telechargerPdf(Demande d) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer la demande");
        fc.setInitialFileName(d.getNumeroDemande() + ".pdf");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File dest = fc.showSaveDialog(tableDemandes.getScene().getWindow());
        if (dest == null) return;
        runAsync(() -> pdfService.genererPdfFichier(d, dest), file -> {
            afficherToast("PDF genere", dest.getName(), true);
            return null;
        });
    }

    // ── Toast ──────────────────────────────────────────────────────────────────
    private void afficherToast(String titre, String sous, boolean success) {
        StackPane cp = getContentPane();
        if (cp == null) return;
        String bg  = success ? "#16a34a" : "#dc2626";
        var ikon    = success ? MaterialDesignC.CHECK_CIRCLE_OUTLINE
                : MaterialDesignI.INFORMATION_OUTLINE;
        VBox toast = new VBox(4);
        toast.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:8;"
                + "-fx-padding:12 16;-fx-effect:dropshadow(gaussian,"
                + "rgba(0,0,0,0.18),12,0,0,4);");
        toast.setMaxWidth(340); toast.setMinWidth(240);
        toast.setMaxHeight(40);
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = FontIcon.of(ikon, 18, Color.WHITE);
        Label tl = new Label(titre);
        tl.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:white;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button cb = new Button();
        cb.setGraphic(FontIcon.of(MaterialDesignC.CLOSE, 14, Color.WHITE));
        cb.setStyle("-fx-background-color:transparent;-fx-padding:0;"
                + "-fx-cursor:hand;-fx-border-color:transparent;");
        row.getChildren().addAll(icon, tl, sp, cb);
        Label sl = new Label(sous);
        sl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);"
                + "-fx-padding:0 0 0 28;");
        sl.setWrapText(true);
        toast.getChildren().addAll(row, sl);
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new Insets(16, 16, 0, 0));
        cp.getChildren().add(toast);
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
            fo.setOnFinished(ev -> cp.getChildren().remove(toast));
            fo.play(); so.play();
        };
        cb.setOnAction(e -> fermer.run());
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
            if (overlay.getParent() instanceof Pane p) p.getChildren().remove(overlay);
        });
        ft.play();
    }
    private StackPane getContentPane() {
        try { return (StackPane) tableDemandes.getScene().lookup("#contentPane"); }
        catch (Exception e) { return null; }
    }
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
