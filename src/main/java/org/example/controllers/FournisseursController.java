package org.example.controllers;

import org.example.models.Fournisseur;
import org.example.services.FournisseurService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
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

public class FournisseursController {

    @FXML private TextField searchField;
    @FXML private FlowPane  cardsPane;
    @FXML private Label     countLabel;
    @FXML private VBox      emptyState;

    private final FournisseurService service = new FournisseurService();

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Recherche en temps réel
        searchField.textProperty().addListener((o, ov, nv) -> {
            String terme = nv == null ? "" : nv.trim();
            if (terme.isEmpty()) chargerDonnees();
            else                 rechercher(terme);
        });
        chargerDonnees();
    }

    // ── Chargement ─────────────────────────────────────────────────────────────
    private void chargerDonnees() {
        runAsync(service::findAll, this::afficherCartes);
    }

    private void rechercher(String terme) {
        runAsync(() -> service.rechercher(terme), this::afficherCartes);
    }

    // ── Affichage des cartes ───────────────────────────────────────────────────
    private Void afficherCartes(List<Fournisseur> liste) {
        cardsPane.getChildren().clear();

        if (liste.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            countLabel.setText("");
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            int count = liste.size();
            countLabel.setText(count + " fournisseur" + (count > 1 ? "s" : ""));
            for (Fournisseur f : liste) {
                cardsPane.getChildren().add(creerCarte(f));
            }
        }
        return null;
    }

    // ── Création d'une carte fournisseur ───────────────────────────────────────
    private VBox creerCarte(Fournisseur f) {
        VBox card = new VBox(0);
        card.getStyleClass().add("fournisseur-card");
        card.setPrefWidth(340);
        card.setMaxWidth(340);

        // ── En-tête de la carte ──────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 16, 12, 16));

        // Avatar avec initiales
        StackPane avatar = new StackPane();
        avatar.setMinSize(44, 44); avatar.setMaxSize(44, 44);
        avatar.setStyle("-fx-background-color:#214293;-fx-background-radius:10;");
        FontIcon icon = new FontIcon("mdi2t-truck-outline");
        icon.setStyle("-fx-icon-color:white;");
        avatar.getChildren().add(icon);

        // Nom + contact
        VBox infoPrincipale = new VBox(2);
        HBox.setHgrow(infoPrincipale, Priority.ALWAYS);
        Label nomLabel = new Label(f.getNom() != null ? f.getNom() : "—");
        nomLabel.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:#111827;");
        nomLabel.setWrapText(true);

        Label contactLabel = new Label(f.getSpecialite() != null && !f.getSpecialite().isBlank()
                ? f.getSpecialite() : "Fournisseur");
        contactLabel.setStyle("-fx-font-size:11.5px;-fx-text-fill:#6b7280;");

        infoPrincipale.getChildren().addAll(nomLabel, contactLabel);

        // Boutons modifier / supprimer
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER);

        Button btnEdit = new Button();
        btnEdit.setGraphic(new FontIcon("mdi2p-pencil-outline:15"));
        btnEdit.getStyleClass().add("card-action-btn-edit");
        btnEdit.setTooltip(new Tooltip("Modifier"));
        btnEdit.setOnAction(e -> ouvrirFormulaire(f));

        Button btnDel = new Button();
        btnDel.setGraphic(new FontIcon("mdi2d-delete-outline:15"));
        btnDel.getStyleClass().add("card-action-btn-del");
        btnDel.setTooltip(new Tooltip("Supprimer"));
        btnDel.setOnAction(e -> confirmerSuppression(f));

        actions.getChildren().addAll(btnEdit, btnDel);
        header.getChildren().addAll(avatar, infoPrincipale, actions);

        // ── Séparateur ───────────────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-opacity:0.5;");

        // ── Détails ──────────────────────────────────────────────────────────
        VBox details = new VBox(8);
        details.setPadding(new Insets(12, 16, 16, 16));

        if (f.getEmail() != null && !f.getEmail().isBlank())
            details.getChildren().add(ligneDetail("mdi2e-email-outline:14", f.getEmail()));

        if (f.getTelephone() != null && !f.getTelephone().isBlank())
            details.getChildren().add(ligneDetail("mdi2p-phone-outline:14", f.getTelephone()));

        if (f.getAdresse() != null && !f.getAdresse().isBlank())
            details.getChildren().add(ligneDetail("mdi2m-map-marker-outline:14", f.getAdresse()));

        // Si aucun détail
        if (details.getChildren().isEmpty()) {
            Label aucun = new Label("Aucun contact renseigné");
            aucun.setStyle("-fx-font-size:11.5px;-fx-text-fill:#d1d5db;-fx-font-style:italic;");
            details.getChildren().add(aucun);
        }

        card.getChildren().addAll(header, sep, details);

        // Animation d'apparition
        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        return card;
    }

    /** Ligne d'info avec icône + texte */
    private HBox ligneDetail(String iconLiteral, String texte) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        FontIcon ic = new FontIcon(iconLiteral);
        ic.setStyle("-fx-icon-color:#9ca3af;");
        Label lbl = new Label(texte);
        lbl.setStyle("-fx-font-size:12.5px;-fx-text-fill:#4b5563;");
        lbl.setWrapText(true);
        row.getChildren().addAll(ic, lbl);
        return row;
    }

    // ── Formulaire modal ────────────────────────────────────────────────────────
    @FXML
    private void handleNouveauFournisseur() { ouvrirFormulaire(null); }

    private void ouvrirFormulaire(Fournisseur f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/views/admin/fournisseur_form.fxml"));

            StackPane overlay = creerOverlay();
            Node form = loader.load();
            FournisseurFormController ctrl = loader.getController();
            ctrl.setFournisseur(f);
            ctrl.setOnSaved(() -> {
                fermerOverlay(overlay);
                chargerDonnees();
                afficherToast(
                        f == null ? "Fournisseur ajouté" : "Fournisseur modifié",
                        f == null ? "Le fournisseur a été créé avec succès."
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
    private void confirmerSuppression(Fournisseur f) {
        StackPane overlay = creerOverlay();

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(340);
        card.setMaxHeight(300);
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;"
                        + "-fx-padding:28 28 24 28;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.16),24,0,0,6);"
        );

        // Icône rouge
        StackPane iconCircle = new StackPane();
        iconCircle.setMinSize(52, 52); iconCircle.setMaxSize(52, 52);
        iconCircle.setStyle("-fx-background-radius:50;-fx-background-color:#fee2e2;");
        FontIcon icon = new FontIcon("mdi2d-delete-outline:24");
        icon.setStyle("-fx-icon-color:#dc2626;");
        iconCircle.getChildren().add(icon);

        Label titre = new Label("Supprimer le fournisseur");
        titre.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#111827;");

        Label corps = new Label("Voulez-vous supprimer « " + f.getNom() + " » ?\nCette action est irréversible.");
        corps.setStyle("-fx-font-size:12.5px;-fx-text-fill:#6b7280;-fx-text-alignment:center;");
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
            runAsync(() -> { service.supprimer(f.getId()); return null; }, v -> {
                chargerDonnees();
                afficherToast("Fournisseur supprimé", f.getNom(), false);
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

    // ── Async helper ───────────────────────────────────────────────────────────
    private <T> void runAsync(SqlSupplier<T> supplier, javafx.util.Callback<T, Void> onSuccess) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return supplier.get(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> onSuccess.call(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() ->
                afficherToast("Erreur", task.getException().getMessage(), false)));
        new Thread(task).start();
    }

    @FunctionalInterface interface SqlSupplier<T> { T get() throws Exception; }
}
