package org.example.controllers;

import org.example.models.Materiel;
import org.example.services.MaterielService;
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
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.List;

public class MaterielsController {

    // ── Stats ──────────────────────────────────────────────────────────────────
    @FXML private Label cardTotal, cardEnService, cardEnPanne, cardAlertes;

    // ── Recherche & filtres ────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterCategorie, filterEtat;

    // ── Table ──────────────────────────────────────────────────────────────────
    @FXML private TableView<Materiel>             tableMateriel;
    @FXML private TableColumn<Materiel, String>   colNom, colReference, colCategorie;
    @FXML private TableColumn<Materiel, String>   colMarque, colFournisseur, colStock, colEtat;
    @FXML private TableColumn<Materiel, Void>     colActions;

    private final MaterielService          service = new MaterielService();
    private final ObservableList<Materiel> data    = FXCollections.observableArrayList();

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurerFiltres();
        configurerTable();
        chargerDonnees();
    }

    private void configurerFiltres() {
        // États fixes
        filterEtat.setItems(FXCollections.observableArrayList(
                "Tous les états", "neuf", "en_service", "en_panne", "reforme"));
        filterEtat.setValue("Tous les états");

        // Catégories chargées depuis la DB
        runAsync(service::findCategories, cats -> {
            List<String> items = new java.util.ArrayList<>();
            items.add("Toutes catégories");
            items.addAll(cats);
            filterCategorie.setItems(FXCollections.observableArrayList(items));
            filterCategorie.setValue("Toutes catégories");
            return null;
        });

        searchField.textProperty().addListener((o, ov, nv)   -> filtrer());
        filterCategorie.valueProperty().addListener((o, ov, nv) -> filtrer());
        filterEtat.valueProperty().addListener((o, ov, nv)     -> filtrer());
    }

    private void configurerTable() {

        // ── Colonne Nom (icône + nom + sous-titre numéro série) ───────────────
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Materiel m = (Materiel) getTableRow().getItem();

                StackPane iconBox = new StackPane();
                iconBox.setMinSize(32, 32); iconBox.setMaxSize(32, 32);
                iconBox.setStyle("-fx-background-color:#e8f0fb;-fx-background-radius:8;");
                FontIcon icon = new FontIcon("mdi2p-package-variant-closed:14");
                icon.setStyle("-fx-icon-color:#0154a6;");
                iconBox.getChildren().add(icon);

                Label nomLbl = new Label(m.getNom() != null ? m.getNom() : "—");
                nomLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#111827;");

                Label serieLbl = new Label(m.getNumeroSerie() != null && !m.getNumeroSerie().isBlank()
                        ? m.getNumeroSerie() : " ");
                serieLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#9ca3af;");

                VBox textBox = new VBox(1, nomLbl, serieLbl);
                HBox box = new HBox(10, iconBox, textBox);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
            }
        });

        // ── Référence ─────────────────────────────────────────────────────────
        colReference.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReference() != null ? c.getValue().getReference() : "—"));
        colReference.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                l.setStyle("-fx-font-family:monospace;-fx-font-size:12px;"
                        + "-fx-text-fill:#374151;-fx-background-color:#f3f4f6;"
                        + "-fx-background-radius:4;-fx-padding:2 6;");
                setGraphic(l); setText(null);
            }
        });

        // ── Catégorie ─────────────────────────────────────────────────────────
        colCategorie.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategorie() != null ? c.getValue().getCategorie() : "—"));

        // ── Marque / Modèle ───────────────────────────────────────────────────
        colMarque.setCellValueFactory(c -> {
            Materiel m = c.getValue();
            String val = "";
            if (m.getMarque() != null && !m.getMarque().isBlank()) val += m.getMarque();
            if (m.getModele() != null && !m.getModele().isBlank())
                val += (val.isEmpty() ? "" : " · ") + m.getModele();
            return new SimpleStringProperty(val.isEmpty() ? "—" : val);
        });

        // ── Fournisseur ───────────────────────────────────────────────────────
        colFournisseur.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNomFournisseur() != null
                        ? c.getValue().getNomFournisseur() : "—"));
        colFournisseur.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                l.setStyle("-fx-text-fill:" + (item.equals("—") ? "#d1d5db" : "#0154a6")
                        + ";-fx-font-size:12.5px;");
                setGraphic(l); setText(null);
            }
        });

        // ── Stock (quantité + alerte visuelle) ────────────────────────────────
        colStock.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getQuantiteStock())));
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Materiel m = (Materiel) getTableRow().getItem();
                boolean alerte = m.estEnAlerte();

                Label qte = new Label(String.valueOf(m.getQuantiteStock()));
                qte.setStyle("-fx-font-weight:bold;-fx-font-size:13px;"
                        + "-fx-text-fill:" + (alerte ? "#dc2626" : "#111827") + ";");

                HBox box = new HBox(4);
                box.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().add(qte);
                if (alerte) {
                    FontIcon alertIcon = new FontIcon("mdi2a-alert:13");
                    alertIcon.setStyle("-fx-icon-color:#dc2626;");
                    box.getChildren().add(alertIcon);
                }
                setGraphic(box); setText(null);
            }
        });

        // ── État badge ────────────────────────────────────────────────────────
        colEtat.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEtat() != null ? c.getValue().getEtat().name() : ""));
        colEtat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Materiel m = (Materiel) getTableRow().getItem();
                Label badge = new Label(m.getEtatLabel());
                badge.getStyleClass().addAll("badge", m.getEtatStyle());
                setGraphic(badge); setText(null);
            }
        });

        // ── Actions ───────────────────────────────────────────────────────────
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Materiel m = (Materiel) getTableRow().getItem();

                Button btnEdit = new Button();
                btnEdit.setGraphic(new FontIcon("mdi2p-pencil-outline:13"));
                btnEdit.getStyleClass().add("btn-table-edit");
                btnEdit.setOnAction(e -> ouvrirFormulaire(m));

                Button btnDel = new Button();
                btnDel.setGraphic(new FontIcon("mdi2d-delete-outline:13"));
                btnDel.getStyleClass().add("btn-table-danger");
                btnDel.setOnAction(e -> confirmerSuppression(m));

                HBox box = new HBox(6, btnEdit, btnDel);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
            }
        });

        tableMateriel.setItems(data);
        tableMateriel.setPlaceholder(new Label("Aucun matériel trouvé."));
        tableMateriel.setFixedCellSize(54);
    }

    // ── Chargement & stats ─────────────────────────────────────────────────────
    private void chargerDonnees() {
        runAsync(service::findAll, list -> {
            data.setAll(list);
            mettreAJourStats();
            return null;
        });
    }

    private void mettreAJourStats() {
        runAsync(() -> new int[]{
                service.countTotal(),
                service.countEnService(),
                service.countEnPanne(),
                service.countAlertes()
        }, s -> {
            cardTotal.setText(String.valueOf(s[0]));
            cardEnService.setText(String.valueOf(s[1]));
            cardEnPanne.setText(String.valueOf(s[2]));
            cardAlertes.setText(String.valueOf(s[3]));
            return null;
        });
    }

    // ── Filtrage ───────────────────────────────────────────────────────────────
    private void filtrer() {
        String terme     = searchField.getText();
        String categorie = (filterCategorie.getValue() == null
                || filterCategorie.getValue().startsWith("Toutes")) ? null : filterCategorie.getValue();
        String etat      = (filterEtat.getValue() == null
                || filterEtat.getValue().startsWith("Tous")) ? null : filterEtat.getValue();

        runAsync(() -> service.rechercher(terme, categorie, etat),
                list -> { data.setAll(list); return null; });
    }

    @FXML private void handleRefresh() {
        searchField.clear();
        filterCategorie.setValue("Toutes catégories");
        filterEtat.setValue("Tous les états");
        chargerDonnees();
    }

    // ── Formulaire ─────────────────────────────────────────────────────────────
    @FXML
    private void handleNouveauMateriel() { ouvrirFormulaire(null); }

    private void ouvrirFormulaire(Materiel m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/views/admin/materiel_form.fxml"));

            StackPane overlay = creerOverlay();
            Node form = loader.load();
            MaterielFormController ctrl = loader.getController();
            ctrl.setMateriel(m);
            ctrl.setOnSaved(() -> {
                fermerOverlay(overlay);
                chargerDonnees();
                afficherToast(
                        m == null ? "Matériel créé" : "Matériel modifié",
                        m == null ? "Le matériel a été ajouté au catalogue."
                                : "Les informations ont été mises à jour.",
                        true
                );
            });
            ctrl.setOnCancelled(() -> fermerOverlay(overlay));

            StackPane.setAlignment(form, Pos.CENTER);
            overlay.getChildren().add(form);
            afficherOverlay(overlay);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Confirm suppression ────────────────────────────────────────────────────
    private void confirmerSuppression(Materiel m) {
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
        iconCircle.setStyle("-fx-background-radius:50;-fx-background-color:#fee2e2;");
        FontIcon icon = new FontIcon("mdi2d-delete-outline:24");
        icon.setStyle("-fx-icon-color:#dc2626;");
        iconCircle.getChildren().add(icon);

        Label titre = new Label("Supprimer ce matériel");
        titre.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#111827;");

        Label corps = new Label("Voulez-vous supprimer « " + m.getNom()
                + " » ?\nCette action est irréversible.");
        corps.setStyle("-fx-font-size:12.5px;-fx-text-fill:#6b7280;"
                + "-fx-text-alignment:center;-fx-wrap-text:true;");
        corps.setWrapText(true);
        corps.setMaxWidth(280);
        corps.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button btnOui = new Button("Supprimer");
        btnOui.getStyleClass().add("btn-table-danger");
        btnOui.setStyle(btnOui.getStyle() + "-fx-padding:8 24;-fx-font-size:13px;");

        Button btnNon = new Button("Annuler");
        btnNon.getStyleClass().add("btn-secondary");
        btnNon.setStyle(btnNon.getStyle() + "-fx-padding:8 24;-fx-font-size:13px;");

        HBox btns = new HBox(10, btnNon, btnOui);
        btns.setAlignment(Pos.CENTER);

        card.getChildren().addAll(iconCircle, titre, corps, btns);
        overlay.getChildren().add(card);

        btnNon.setOnAction(e -> fermerOverlay(overlay));
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) fermerOverlay(overlay); });
        btnOui.setOnAction(e -> {
            fermerOverlay(overlay);
            runAsync(() -> { service.supprimer(m.getId()); return null; }, v -> {
                chargerDonnees();
                afficherToast("Matériel supprimé", m.getNom(), false);
                return null;
            });
        });

        afficherOverlay(overlay);
    }

    // ── Toast haut-droite ──────────────────────────────────────────────────────
    private void afficherToast(String titre, String sousTitre, boolean success) {
        StackPane contentPane = getContentPane();
        if (contentPane == null) return;

        // ── Couleurs selon le type ──
        String bgColor = success ? "#16a34a" : "#dc2626";      // vert ou rouge
        String iconLit  = success ? "mdi2c-check-circle-outline:18" : "mdi2c-information-outline:18";

        // ── Conteneur principal ──
        VBox toast = new VBox(4);
        toast.setStyle(
                "-fx-background-color: " + bgColor + ";"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 12 16 12 16;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 4);"
        );
        toast.setMaxWidth(300);
        toast.setMinWidth(240);
        toast.setMaxHeight(40);

        // ── Ligne titre + icône ──
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconLit);
        icon.setStyle("-fx-icon-color: white;");

        Label titleLabel = new Label(titre);
        titleLabel.setStyle(
                "-fx-font-weight: bold;"
                        + "-fx-font-size: 14px;"
                        + "-fx-text-fill: white;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(new FontIcon("mdi2c-close:14"));
        closeBtn.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-padding: 0;"
                        + "-fx-cursor: hand;"
                        + "-fx-border-color: transparent;"
                        + "-fx-text-fill: white;"
        );
        closeBtn.setGraphic(new FontIcon("mdi2c-close:14"));
        ((FontIcon) closeBtn.getGraphic()).setStyle("-fx-icon-color: white;");

        headerRow.getChildren().addAll(icon, titleLabel, spacer, closeBtn);

        // ── Sous-titre ──
        Label subLabel = new Label(sousTitre);
        subLabel.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: rgba(255,255,255,0.85);"
                        + "-fx-padding: 0 0 0 28;"       // aligné avec l'icône
        );
        subLabel.setWrapText(true);

        // ── Assemblage ──
        toast.getChildren().addAll(headerRow, subLabel);

        // ── Position : en haut à droite ──
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new Insets(16, 16, 0, 0));

        contentPane.getChildren().add(toast);

        // ── Animation d'entrée ──
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), toast);
        slideIn.setFromX(320); slideIn.setToX(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        slideIn.play(); fadeIn.play();

        // ── Fermeture automatique après 3,5s ──
        Runnable fermer = () -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), toast);
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(250), toast);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            slideOut.setFromX(0); slideOut.setToX(320);
            fadeOut.setOnFinished(ev -> contentPane.getChildren().remove(toast));
            fadeOut.play(); slideOut.play();
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
            if (overlay.getParent() instanceof Pane p) p.getChildren().remove(overlay);
        });
        ft.play();
    }

    private StackPane getContentPane() {
        try { return (StackPane) searchField.getScene().lookup("#contentPane"); }
        catch (Exception e) { return null; }
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
