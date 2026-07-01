package org.example.controllers;

import org.example.models.Utilisateur;
import org.example.services.UtilisateurService;
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

public class GestionUtilisateursController {

    // ── Stats ──────────────────────────────────────────────────────────────────
    @FXML private Label cardTotal, cardActifs, cardBloques, cardIntervenants;

    // ── Filtres ────────────────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterRole, filterStatut;

    // ── Table ──────────────────────────────────────────────────────────────────
    @FXML private TableView<Utilisateur>           tableUtilisateurs;
    @FXML private TableColumn<Utilisateur, String> colNom, colEmail, colRole, colSite, colStatut;
    @FXML private TableColumn<Utilisateur, Void>   colActions;

    private final UtilisateurService         service = new UtilisateurService();
    private final ObservableList<Utilisateur> data    = FXCollections.observableArrayList();

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurerFiltres();
        configurerTable();
        chargerDonnees();
    }

    private void configurerFiltres() {
        filterRole.setItems(FXCollections.observableArrayList("Tous les rôles", "admin", "intervenant"));
        filterRole.setValue("Tous les rôles");
        filterStatut.setItems(FXCollections.observableArrayList("Tous les statuts", "actif", "inactif", "bloque"));
        filterStatut.setValue("Tous les statuts");
        searchField.textProperty().addListener((o, ov, nv) -> filtrer());
        filterRole.valueProperty().addListener((o, ov, nv) -> filtrer());
        filterStatut.valueProperty().addListener((o, ov, nv) -> filtrer());
    }

    private void configurerTable() {
        // ── Colonne Utilisateur (avatar initiales + nom) ────────────────────
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomComplet()));
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Utilisateur u = (Utilisateur) getTableRow().getItem();
                String initials = (u.getPrenom().isEmpty() ? "?" : String.valueOf(u.getPrenom().charAt(0)))
                        + (u.getNom().isEmpty() ? "" : String.valueOf(u.getNom().charAt(0)));
                Label av = new Label(initials.toUpperCase());
                av.setStyle("-fx-background-color:#e8f0fb;-fx-text-fill:#0154a6;"
                        + "-fx-font-weight:bold;-fx-font-size:12px;-fx-background-radius:50;"
                        + "-fx-min-width:32px;-fx-max-width:32px;-fx-min-height:32px;-fx-max-height:32px;"
                        + "-fx-alignment:center;");
                Label nom = new Label(u.getNomComplet());
                nom.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#111827;");
                HBox box = new HBox(10, av, nom);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
            }
        });

        // ── Colonne Email ───────────────────────────────────────────────────
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEmail.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label l = new Label(item);
                l.setStyle("-fx-text-fill:#6b7280;-fx-font-size:12.5px;");
                setGraphic(l); setText(null);
            }
        });

        // ── Colonne Rôle ────────────────────────────────────────────────────
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().name()));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item.equals("admin") ? "Admin" : "Intervenant");
                badge.getStyleClass().addAll("badge", item.equals("admin") ? "badge-admin" : "badge-interv");
                setGraphic(badge); setText(null);
            }
        });

        // ── Colonne Site ────────────────────────────────────────────────────
        colSite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSiteAffecte()));

        // ── Colonne Statut ──────────────────────────────────────────────────
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut().name()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String txt = switch (item) {
                    case "actif"  -> "Actif";
                    case "bloque" -> "Bloqué";
                    default       -> "Inactif";
                };
                Label badge = new Label(txt);
                badge.getStyleClass().addAll("badge", "badge-" + item);
                setGraphic(badge); setText(null);
            }
        });

        // ── Colonne Actions ─────────────────────────────────────────────────
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Utilisateur u   = (Utilisateur) getTableRow().getItem();
                boolean bloque  = u.getStatut() == Utilisateur.Statut.bloque;

                Button btnEdit   = new Button();
                Button btnBlock  = new Button();
                Button btnDelete = new Button();

                btnEdit.getStyleClass().add("btn-table-edit");
                btnBlock.getStyleClass().add(bloque ? "btn-table-unblock" : "btn-table-warn");
                btnDelete.getStyleClass().add("btn-table-danger");

                // Icônes dans les boutons
                btnEdit.setGraphic(new FontIcon("mdi2p-pencil-outline:14"));
                btnBlock.setGraphic(new FontIcon(bloque ? "mdi2l-lock-open-outline:14" : "mdi2l-lock-outline:14"));
                btnDelete.setGraphic(new FontIcon("mdi2d-delete-outline:14"));

                btnEdit.setOnAction(e -> ouvrirFormulaire(u));
                btnBlock.setOnAction(e -> confirmerBlocage(u));
                btnDelete.setOnAction(e -> confirmerSuppression(u));

                HBox box = new HBox(6, btnEdit, btnBlock, btnDelete);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
            }
        });

        tableUtilisateurs.setItems(data);
        tableUtilisateurs.setPlaceholder(new Label("Aucun utilisateur trouvé."));
        tableUtilisateurs.setFixedCellSize(54);
    }

    // ── Chargement ─────────────────────────────────────────────────────────────
    private void chargerDonnees() {
        runAsync(() -> service.findAll(), list -> {
            data.setAll(list);
            mettreAJourStats();
            return null;
        });
    }

    private void mettreAJourStats() {
        runAsync(() -> new int[]{
                service.countTotal(), service.countActifs(),
                service.countBloques(), service.countIntervenants()
        }, s -> {
            cardTotal.setText(String.valueOf(s[0]));
            cardActifs.setText(String.valueOf(s[1]));
            cardBloques.setText(String.valueOf(s[2]));
            cardIntervenants.setText(String.valueOf(s[3]));
            return null;
        });
    }

    // ── Filtrage ───────────────────────────────────────────────────────────────
    private void filtrer() {
        String terme  = searchField.getText();
        String role   = filterRole.getValue().startsWith("Tous")   ? null : filterRole.getValue();
        String statut = filterStatut.getValue().startsWith("Tous") ? null : filterStatut.getValue();
        runAsync(() -> service.rechercher(terme, role, statut),
                list -> { data.setAll(list); return null; });
    }

    @FXML private void handleRefresh() {
        searchField.clear();
        filterRole.setValue("Tous les rôles");
        filterStatut.setValue("Tous les statuts");
        chargerDonnees();
    }

    // ── Formulaire modal ────────────────────────────────────────────────────────
    @FXML
    private void handleNouvelUtilisateur() { ouvrirFormulaire(null); }

    private void ouvrirFormulaire(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/views/admin/utilisateur_form.fxml"));

            StackPane overlay = creerOverlay();
            Node form = loader.load();
            UtilisateurFormController ctrl = loader.getController();
            ctrl.setUtilisateur(u);
            ctrl.setOnSaved(() -> {
                fermerOverlay(overlay);
                chargerDonnees();
                afficherToast(
                        u == null ? "Utilisateur créé" : "Utilisateur modifié",
                        u == null ? "Le compte a été créé avec succès." : "Les informations ont été mises à jour.",
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

    // ── Confirm — compact, centré, pas trop grand ───────────────────────────────
    private void confirmerBlocage(Utilisateur u) {
        boolean estBloque = u.getStatut() == Utilisateur.Statut.bloque;

        afficherConfirm(
                estBloque ? "mdi2a-account-off" : "mdi2a-account-check",
                estBloque ? "#d97706" : "#d97706",
                (estBloque ? "Débloquer" : "Bloquer") + " le compte",
                "Voulez-vous " + (estBloque ? "débloquer" : "bloquer") + " le compte de "
                        + u.getNomComplet() + " ?",
                estBloque ? "btn-table-unblock" : "btn-table-warn",
                "Confirmer",
                () -> runAsync(() -> {
                    if (estBloque) service.debloquer(u.getId());
                    else           service.bloquer(u.getId());
                    return null;
                }, v -> {
                    chargerDonnees();
                    afficherToast(
                            "Compte " + (estBloque ? "débloqué" : "bloqué"),
                            u.getNomComplet(),
                            true
                    );
                    return null;
                })
        );
    }

    private void confirmerSuppression(Utilisateur u) {
        afficherConfirm(
                "mdi2t-trash-can-outline",
                "#dc2626",
                "Supprimer le compte",
                "Cette action est irréversible. Supprimer " + u.getNomComplet() + " ?",
                "btn-table-danger",
                "Supprimer",
                () -> runAsync(() -> { service.supprimer(u.getId()); return null; }, v -> {
                    chargerDonnees();
                    afficherToast("Compte supprimé", u.getNomComplet(), false);
                    return null;
                })
        );
    }

    /**
     * Dialog de confirmation compact et centré.
     * Largeur max 340px.
     */
    private void afficherConfirm(String iconLiteral, String iconColor, String titre,
                                 String corps, String btnStyle, String btnLabel,
                                 Runnable onConfirm) {
        StackPane overlay = creerOverlay();

        // Card compacte
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(340);
        card.setMaxHeight(300);
        card.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 14;"
                        + "-fx-padding: 28 28 24 28;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.16), 24, 0, 0, 6);"
        );

        // Icône centrée
        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(48, 48); iconCircle.setMaxSize(48, 48);
        iconCircle.setStyle("-fx-background-radius: 50; -fx-background-color: "
                + (iconColor.equals("#dc2626") ? "#fee2e2" : "#fef3c7") + ";");
        FontIcon icon = new FontIcon(iconLiteral + ":22");
        icon.setStyle("-fx-icon-color: " + iconColor + ";");
        iconCircle.getChildren().add(icon);

        // Titre
        Label titleLbl = new Label(titre);
        titleLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        titleLbl.setWrapText(true);
        titleLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Corps
        Label bodyLbl = new Label(corps);
        bodyLbl.setStyle("-fx-font-size:12.5px;-fx-text-fill:#6b7280;-fx-wrap-text:true;"
                + "-fx-text-alignment:center;");
        bodyLbl.setWrapText(true);
        bodyLbl.setMaxWidth(280);
        bodyLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Boutons
        Button btnOui = new Button(btnLabel);
        btnOui.getStyleClass().add(btnStyle);
        btnOui.setStyle(btnOui.getStyle() + "-fx-padding:8 22;-fx-font-size:13px;");

        Button btnNon = new Button("Annuler");
        btnNon.getStyleClass().add("btn-secondary");
        btnNon.setStyle(btnNon.getStyle() + "-fx-padding:8 22;-fx-font-size:13px;");

        HBox btns = new HBox(10, btnNon, btnOui);
        btns.setAlignment(Pos.CENTER);

        card.getChildren().addAll(iconCircle, titleLbl, bodyLbl, btns);
        overlay.getChildren().add(card);

        btnNon.setOnAction(e -> fermerOverlay(overlay));
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) fermerOverlay(overlay); });
        btnOui.setOnAction(e -> { fermerOverlay(overlay); onConfirm.run(); });

        afficherOverlay(overlay);
    }

    // ── Toast — haut à droite, petit et rectangulaire ─────────────────────────
    private void afficherToast(String titre, String sousTitre, boolean success) {
        StackPane contentPane = getContentPane();
        if (contentPane == null) return;

        // ── Couleurs selon le type ──
        String bgColor = success ? "#16a34a" : "#dc2626";      // vert ou rouge
        String iconLit  = success ? "mdi2c-check-circle-outline:18" : "mdi2i-information-outline:18";

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
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        return overlay;
    }

    private void afficherOverlay(StackPane overlay) {
        StackPane contentPane = getContentPane();
        if (contentPane == null) return;
        contentPane.getChildren().add(overlay);
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
        try {
            return (StackPane) tableUtilisateurs.getScene().lookup("#contentPane");
        } catch (Exception e) { return null; }
    }

    // ── Helper async ───────────────────────────────────────────────────────────
    private <T> void runAsync(SqlSupplier<T> supplier, javafx.util.Callback<T, Void> onSuccess) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return supplier.get(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> onSuccess.call(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() ->
                afficherToast("Erreur", task.getException().getMessage(), false)));
        new Thread(task).start();
    }

    @FunctionalInterface
    interface SqlSupplier<T> { T get() throws Exception; }
}