package org.example.controllers;

import org.example.models.Utilisateur;
import org.example.services.NotificationService;
import org.example.utils.SessionManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class TopbarController {

    private static TopbarController instance;

    @FXML private Label     pageTitle;
    @FXML private Label     notifBadge;
    @FXML private Button    themeBtn;
    @FXML private FontIcon  themeIcon;
    @FXML private Label     avatarInitials;
    @FXML private Label     userName;
    @FXML private Label     userRole;
    @FXML private StackPane topbarAvatarPane;
    @FXML private ImageView topbarAvatarImage;

    // NOUVEAUX CHAMPS POUR LES NOTIFICATIONS
    @FXML private Button    notifBtn;                     // bouton cloche
    private boolean panelOuvert = false;
    private StackPane notifOverlay = null;
    private final NotificationService notifService = new NotificationService();

    private boolean darkMode = false;

    @FXML
    public void initialize() {
        instance = this;

        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        if (u == null) return;

        userName.setText(u.getNomComplet());
        userRole.setText(u.isAdmin() ? "Administrateur" : "Intervenant");

        String initials = (u.getPrenom().isEmpty() ? "?"
                : String.valueOf(u.getPrenom().charAt(0)))
                + (u.getNom().isEmpty() ? ""
                : String.valueOf(u.getNom().charAt(0)));
        avatarInitials.setText(initials.toUpperCase());

        // --- Gestion photo de profil (inchangée) ---
        if (u.getFaceEncoding() != null && !u.getFaceEncoding().isBlank()
                && topbarAvatarImage != null) {
            try {
                byte[] bytes = Base64.getDecoder().decode(u.getFaceEncoding());
                Image img = new Image(new ByteArrayInputStream(bytes));
                org.example.utils.CircularImageUtil.appliquer(topbarAvatarImage, img, 36);
                avatarInitials.setVisible(false);
                avatarInitials.setManaged(false);
            } catch (Exception e) {
                // Garder les initiales si la photo ne charge pas
            }
        } else if (topbarAvatarImage != null) {
            topbarAvatarImage.setImage(null);
            topbarAvatarImage.setClip(null);
            topbarAvatarImage.setVisible(false);
            topbarAvatarImage.setManaged(false);
            avatarInitials.setVisible(true);
            avatarInitials.setManaged(true);
        }

        // --- Chargement du badge de notifications ---
        rafraichirBadge();
    }

    // --- Méthodes existantes (inchangées) ---
    public static TopbarController getInstance() {
        return instance;
    }

    public void setPageTitle(String title) {
        pageTitle.setText(title);
    }

    public void setNotifCount(int count) {
        notifBadge.setVisible(count > 0);
        notifBadge.setManaged(count > 0);
        notifBadge.setText(count > 9 ? "9+" : String.valueOf(count));
    }

    public void rafraichirAvatar() {
        initialize(); // inchangé
    }

    // --- Gestion du thème (inchangée) ---
    @FXML
    private void toggleTheme() {
        darkMode = !darkMode;
        Scene scene = themeBtn.getScene();
        if (darkMode) {
            scene.getRoot().getStyleClass().add("dark-theme");
            themeIcon.setIconLiteral("mdi2w-white-balance-sunny");
        } else {
            scene.getRoot().getStyleClass().remove("dark-theme");
            themeIcon.setIconLiteral("mdi2m-moon-waning-crescent");
        }
    }

    // ================================================================
    //  NOUVELLES MÉTHODES POUR LES NOTIFICATIONS (issues du second code)
    // ================================================================

    /** Rafraîchit le badge de notification (appel asynchrone) */
    public void rafraichirBadge() {
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        if (u == null) return;
        Task<Integer> task = new Task<>() {
            @Override protected Integer call() throws Exception {
                return notifService.countNonLues(u.getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() ->
                setNotifCount(task.getValue())));
        new Thread(task).start();
    }

    /** Gestion du clic sur la cloche : ouvre ou ferme le panneau */
    @FXML
    private void handleNotif() {
        if (panelOuvert) {
            fermerPanneauNotif();
        } else {
            ouvrirPanneauNotif();
        }
    }

    private void ouvrirPanneauNotif() {
        try {
            StackPane contentPane = (StackPane) notifBtn.getScene()
                    .lookup("#contentPane");
            if (contentPane == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/views/components/notification_panel.fxml"));
            Node panel = loader.load();
            NotificationPanelController ctrl = loader.getController();

            // Overlay transparent pour fermer le panneau en cliquant à côté
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color:transparent;");
            overlay.setOnMouseClicked(e -> {
                if (e.getTarget() == overlay) fermerPanneauNotif();
            });

            StackPane.setAlignment(panel, Pos.TOP_RIGHT);
            StackPane.setMargin(panel, new javafx.geometry.Insets(8, 16, 0, 0));

            overlay.getChildren().add(panel);
            contentPane.getChildren().add(overlay);
            notifOverlay = overlay;
            panelOuvert = true;

            // Animation d'apparition
            panel.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(180), panel);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            // Callbacks du panneau
            ctrl.setOnClose(this::fermerPanneauNotif);
            ctrl.setOnBadgeUpdate(this::rafraichirBadge);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fermerPanneauNotif() {
        if (notifOverlay == null) {
            panelOuvert = false;
            return;
        }

        if (!notifOverlay.getChildren().isEmpty()) {
            Node panel = notifOverlay.getChildren().get(0);
            FadeTransition ft = new FadeTransition(Duration.millis(150), panel);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> {
                if (notifOverlay.getParent() instanceof Pane p)
                    p.getChildren().remove(notifOverlay);
                notifOverlay = null;
                panelOuvert = false;
                rafraichirBadge();
            });
            ft.play();
        } else {
            if (notifOverlay.getParent() instanceof Pane p)
                p.getChildren().remove(notifOverlay);
            notifOverlay = null;
            panelOuvert = false;
        }
    }
}