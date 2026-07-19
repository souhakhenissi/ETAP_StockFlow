package org.example.controllers;

import org.example.services.NotificationService;
import org.example.services.NotificationService.NotificationItem;
import org.example.utils.SessionManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import java.util.List;

public class NotificationPanelController {

    @FXML private VBox    panelRoot;
    @FXML private Label   titreLabel;
    @FXML private Button  btnToutMarquer;
    @FXML private VBox    listeContainer;
    @FXML private VBox    emptyLabel;
    @FXML private Label   countLabel;

    private final NotificationService service = new NotificationService();
    private Runnable onClose;
    private Runnable onBadgeUpdate;

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        chargerNotifications();
    }

    private void chargerNotifications() {
        int idUser = SessionManager.getInstance().getUtilisateurConnecte().getId();
        Task<List<NotificationItem>> task = new Task<>() {
            @Override protected List<NotificationItem> call() throws Exception {
                return service.findByUser(idUser);
            }
        };
        task.setOnSucceeded(e -> {
            List<NotificationItem> notifs = task.getValue();
            Platform.runLater(() -> afficherNotifications(notifs));
        });
        task.setOnFailed(e ->
                System.err.println("[Notif] Chargement echoue : "
                        + task.getException().getMessage()));
        new Thread(task).start();
    }

    private void afficherNotifications(List<NotificationItem> notifs) {
        listeContainer.getChildren().clear();

        long nonLues = notifs.stream().filter(n -> !n.lue).count();
        countLabel.setText(nonLues > 0
                ? nonLues + " non lue" + (nonLues > 1 ? "s" : "")
                : "Tout est lu");
        countLabel.setStyle("-fx-font-size:11px;-fx-text-fill:"
                + (nonLues > 0 ? "#d97706" : "#6b7280") + ";");

        btnToutMarquer.setVisible(nonLues > 0);
        btnToutMarquer.setManaged(nonLues > 0);

        if (notifs.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            for (NotificationItem n : notifs) {
                listeContainer.getChildren().add(creerLigneNotif(n));
            }
        }
    }

    private VBox creerLigneNotif(NotificationItem n) {
        VBox row = new VBox(4);
        row.setPadding(new Insets(12, 16, 12, 14));
        row.setCursor(javafx.scene.Cursor.HAND);
        row.setStyle(n.lue
                ? "-fx-background-color:white;-fx-border-color:transparent transparent #f3f4f6 transparent;-fx-border-width:0 0 1 0;"
                : "-fx-background-color:#f8faff;-fx-border-color:transparent transparent #e2e8f0 transparent;-fx-border-width:0 0 1 0;");

        // Indicateur non lue (barre bleue à gauche)
        if (!n.lue) {
            row.setStyle(row.getStyle()
                    + "-fx-border-left-color:#0154a6;-fx-padding:12 16 12 11;");
        }

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icône selon le type
        FontIcon icon = switch (n.type) {
            case "demande" -> FontIcon.of(
                    MaterialDesignF.FILE_DOCUMENT_OUTLINE, 14, Color.web("#0154a6"));
            case "stock"   -> FontIcon.of(
                    MaterialDesignP.PACKAGE_VARIANT_CLOSED, 14, Color.web("#d97706"));
            default        -> FontIcon.of(
                    MaterialDesignI.INFORMATION_OUTLINE, 14, Color.web("#6b7280"));
        };

        Label titreLbl = new Label(n.titre);
        titreLbl.setStyle("-fx-font-weight:bold;-fx-font-size:12.5px;"
                + "-fx-text-fill:" + (n.lue ? "#374151" : "#111827") + ";");
        titreLbl.setWrapText(true);
        HBox.setHgrow(titreLbl, Priority.ALWAYS);

        // Temps relatif
        Label tempLbl = new Label(n.getTempsRelatif());
        tempLbl.setStyle("-fx-font-size:10.5px;-fx-text-fill:#9ca3af;");

        // Bouton supprimer
        Button btnDel = new Button();
        btnDel.setGraphic(FontIcon.of(MaterialDesignC.CLOSE, 11, Color.web("#9ca3af")));
        btnDel.setStyle("-fx-background-color:transparent;-fx-padding:2;"
                + "-fx-cursor:hand;-fx-border-color:transparent;");
        btnDel.setVisible(false); // affiché au survol
        btnDel.setOnAction(e -> {
            e.consume();
            supprimerNotif(n, row);
        });

        header.getChildren().addAll(icon, titreLbl, tempLbl, btnDel);

        Label msgLbl = new Label(n.message);
        msgLbl.setStyle("-fx-font-size:11.5px;-fx-text-fill:#6b7280;-fx-padding:0 0 0 22;");
        msgLbl.setWrapText(true);

        row.getChildren().addAll(header, msgLbl);

        // Hover : afficher le bouton supprimer + changer fond
        row.setOnMouseEntered(e -> {
            btnDel.setVisible(true);
            if (n.lue) row.setStyle(row.getStyle().replace(
                    "-fx-background-color:white", "-fx-background-color:#f9fafb"));
        });
        row.setOnMouseExited(e -> {
            btnDel.setVisible(false);
            if (n.lue) row.setStyle(row.getStyle().replace(
                    "-fx-background-color:#f9fafb", "-fx-background-color:white"));
        });

        // Clic : marquer comme lue
        row.setOnMouseClicked(e -> {
            if (!n.lue) marquerLue(n, row);
        });

        return row;
    }

    // ── Actions ────────────────────────────────────────────────────────────────
    private void marquerLue(NotificationItem n, VBox row) {
        n.lue = true;
        row.setStyle("-fx-background-color:white;"
                + "-fx-border-color:transparent transparent #f3f4f6 transparent;"
                + "-fx-border-width:0 0 1 0;-fx-padding:12 16;");

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.marquerLue(n.id);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            chargerNotifications();
            if (onBadgeUpdate != null) onBadgeUpdate.run();
        });
        new Thread(task).start();
    }

    @FXML
    private void handleToutMarquer() {
        int idUser = SessionManager.getInstance().getUtilisateurConnecte().getId();
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.marquerToutesLues(idUser);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            chargerNotifications();
            if (onBadgeUpdate != null) onBadgeUpdate.run();
        });
        new Thread(task).start();
    }

    private void supprimerNotif(NotificationItem n, VBox row) {
        // Animation de sortie
        FadeTransition ft = new FadeTransition(Duration.millis(200), row);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            listeContainer.getChildren().remove(row);
            if (listeContainer.getChildren().isEmpty()) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
            }
        });
        ft.play();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.supprimer(n.id);
                return null;
            }
        };
        task.setOnSucceeded(ev -> {
            if (onBadgeUpdate != null) onBadgeUpdate.run();
        });
        new Thread(task).start();
    }

    @FXML
    private void handleFermer() {
        if (onClose != null) onClose.run();
    }

    public void setOnClose(Runnable r)       { this.onClose = r; }
    public void setOnBadgeUpdate(Runnable r) { this.onBadgeUpdate = r; }
}
