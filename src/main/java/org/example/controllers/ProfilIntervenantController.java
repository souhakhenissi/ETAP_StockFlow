package org.example.controllers;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.example.config.DatabaseConfig;
import org.example.models.Utilisateur;
import org.example.services.UtilisateurService;
import org.example.utils.PasswordUtil;
import org.example.utils.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.PreparedStatement;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProfilIntervenantController {

    // ── Avatar ─────────────────────────────────────────────────────────────────
    @FXML private StackPane   avatarPane;
    @FXML private ImageView   avatarImage;
    @FXML private Label       avatarInitiales;
    @FXML private Button      btnChangerPhoto;

    // ── Infos lecture seule ────────────────────────────────────────────────────
    @FXML private Label       lblNomComplet;
    @FXML private Label       lblRole;
    @FXML private Label       lblSite;

    // ── Formulaire email ───────────────────────────────────────────────────────
    @FXML private TextField   emailField;
    @FXML private Label       errEmail;
    @FXML private Button      btnSauvegarderEmail;

    // ── Formulaire mot de passe ────────────────────────────────────────────────
    @FXML private PasswordField ancienMdpField, nouveauMdpField, confirmMdpField;
    @FXML private Label         errAncienMdp, errNouveauMdp, errConfirmMdp;
    @FXML private Button        btnSauvegarderMdp;

    // ── Webcam (photo profil via caméra) ───────────────────────────────────────
    @FXML private VBox        cameraPane;
    @FXML private ImageView   cameraView;
    @FXML private Button      btnStartCamera, btnCapture, btnFermerCamera;

    private OpenCVFrameGrabber   grabber;
    private Java2DFrameConverter converter    = new Java2DFrameConverter();
    private Thread               cameraThread;
    private final AtomicBoolean  cameraActive = new AtomicBoolean(false);
    private volatile BufferedImage lastFrame;

    // ── Toast container ────────────────────────────────────────────────────────
    @FXML private StackPane   toastContainer;

    private final UtilisateurService service = new UtilisateurService();
    private String photoBase64 = null;

    // ── Init ───────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        if (u == null) return;

        lblNomComplet.setText(u.getNomComplet());
        lblRole.setText(u.isAdmin() ? "Administrateur" : "Intervenant");
        lblSite.setText(u.getSiteAffecte() != null ? u.getSiteAffecte() : "—");
        emailField.setText(u.getEmail());

        // Afficher la photo si elle existe
        if (u.getFaceEncoding() != null && !u.getFaceEncoding().isBlank()) {
            afficherPhotoDepuisBase64(u.getFaceEncoding());
        } else {
            String initiales = (u.getPrenom().isEmpty() ? "?"
                    : String.valueOf(u.getPrenom().charAt(0)))
                    + (u.getNom().isEmpty() ? ""
                    : String.valueOf(u.getNom().charAt(0)));
            avatarInitiales.setText(initiales.toUpperCase());
            avatarImage.setVisible(false);
            avatarImage.setManaged(false);
        }

        // Masquer la caméra au départ
        if (cameraPane != null) {
            cameraPane.setVisible(false);
            cameraPane.setManaged(false);
        }

        errEmail.setVisible(false);    errEmail.setManaged(false);
        errAncienMdp.setVisible(false); errAncienMdp.setManaged(false);
        errNouveauMdp.setVisible(false); errNouveauMdp.setManaged(false);
        errConfirmMdp.setVisible(false); errConfirmMdp.setManaged(false);
    }

    // ── Photo profil ───────────────────────────────────────────────────────────
    @FXML
    private void handleChangerPhoto() {
        // Proposer : fichier ou webcam
        ContextMenu menu = new ContextMenu();

        MenuItem itemFichier = new MenuItem("  Importer une image");
        itemFichier.setGraphic(FontIcon.of(
                MaterialDesignF.FILE_IMAGE_OUTLINE, 14, Color.web("#0154a6")));
        itemFichier.setOnAction(e -> importerImageFichier());

        MenuItem itemCamera = new MenuItem("  Prendre une photo");
        itemCamera.setGraphic(FontIcon.of(
                MaterialDesignC.CAMERA_OUTLINE, 14, Color.web("#0154a6")));
        itemCamera.setOnAction(e -> ouvrirCamera());

        if (photoBase64 != null) {
            MenuItem itemSuppr = new MenuItem("  Supprimer la photo");
            itemSuppr.setGraphic(FontIcon.of(
                    MaterialDesignD.DELETE_OUTLINE, 14, Color.web("#dc2626")));
            itemSuppr.setOnAction(e -> supprimerPhoto());
            menu.getItems().addAll(itemFichier, itemCamera, new SeparatorMenuItem(), itemSuppr);
        } else {
            menu.getItems().addAll(itemFichier, itemCamera);
        }

        menu.show(btnChangerPhoto,
                javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private void importerImageFichier() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        java.io.File file = fc.showOpenDialog(btnChangerPhoto.getScene().getWindow());
        if (file == null) return;
        try {
            Image img = new Image(file.toURI().toString());
            BufferedImage bi = ImageIO.read(file);
            appliquerPhoto(img, bi);
        } catch (IOException e) {
            afficherToast("Erreur", "Impossible de charger l'image.", false);
        }
    }

    private void ouvrirCamera() {
        if (cameraPane == null) return;
        cameraPane.setVisible(true);
        cameraPane.setManaged(true);
        btnStartCamera.setDisable(false);
        btnCapture.setDisable(true);
        cameraView.setImage(null);
    }

    @FXML
    private void startCamera() {
        btnStartCamera.setDisable(true);
        cameraActive.set(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                grabber = new OpenCVFrameGrabber(0);
                grabber.setImageWidth(320); grabber.setImageHeight(240);
                grabber.start();
                while (cameraActive.get()) {
                    Frame frame = grabber.grab();
                    if (frame == null) continue;
                    BufferedImage img = converter.convert(frame);
                    if (img != null) {
                        lastFrame = img;
                        javafx.scene.image.WritableImage fx =
                                SwingFXUtils.toFXImage(img, null);
                        Platform.runLater(() -> cameraView.setImage(fx));
                    }
                    Thread.sleep(33);
                }
                return null;
            }
        };
        task.setOnFailed(e -> Platform.runLater(() -> {
            btnStartCamera.setDisable(false);
            afficherToast("Erreur", "Camera non disponible.", false);
        }));
        cameraThread = new Thread(task);
        cameraThread.setDaemon(true);
        cameraThread.start();
        btnCapture.setDisable(false);
    }

    @FXML
    private void capturePhoto() {
        if (lastFrame == null) return;
        BufferedImage captured = lastFrame;
        stopCamera();
        javafx.scene.image.WritableImage fx = SwingFXUtils.toFXImage(captured, null);
        appliquerPhoto(fx, captured);
        fermerCamera();
    }

    @FXML
    private void fermerCamera() {
        stopCamera();
        if (cameraPane != null) {
            cameraPane.setVisible(false);
            cameraPane.setManaged(false);
        }
    }

    private void stopCamera() {
        cameraActive.set(false);
        if (cameraThread != null) cameraThread.interrupt();
        if (grabber != null) {
            try { grabber.stop(); grabber.release(); }
            catch (Exception ignored) {}
        }
    }

    private void appliquerPhoto(javafx.scene.image.Image fxImg, BufferedImage bi) {
        // Afficher dans l'avatar (cercle, recadrage centré)
        org.example.utils.CircularImageUtil.appliquer(avatarImage, fxImg, 90);
        avatarInitiales.setVisible(false);
        avatarInitiales.setManaged(false);

        // Encoder en base64
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bi, "png", baos);
            photoBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            afficherToast("Erreur", "Encodage de la photo echoue.", false);
        }

        // Sauvegarder immédiatement en DB
        sauvegarderPhoto();
    }

    private void sauvegarderPhoto() {
        int id = SessionManager.getInstance().getUtilisateurConnecte().getId();
        String enc = photoBase64;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.enregistrerFaceEncoding(id, enc);
                SessionManager.getInstance().getUtilisateurConnecte()
                        .setFaceEncoding(enc);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            afficherToast("Photo mise a jour",
                    "Votre photo de profil a ete enregistree.", true);
            if (TopbarController.getInstance() != null) {
                TopbarController.getInstance().rafraichirAvatar();
            }
        });
        task.setOnFailed(e -> afficherToast("Erreur",
                "Impossible de sauvegarder la photo.", false));
        new Thread(task).start();
    }

    private void supprimerPhoto() {
        photoBase64 = null;
        avatarImage.setImage(null);
        avatarImage.setClip(null);
        avatarImage.setVisible(false);
        avatarImage.setManaged(false);
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        String initiales = (u.getPrenom().isEmpty() ? "?"
                : String.valueOf(u.getPrenom().charAt(0)))
                + (u.getNom().isEmpty() ? ""
                : String.valueOf(u.getNom().charAt(0)));
        avatarInitiales.setText(initiales.toUpperCase());
        avatarInitiales.setVisible(true);
        avatarInitiales.setManaged(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.enregistrerFaceEncoding(u.getId(), null);
                u.setFaceEncoding(null);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            afficherToast("Photo supprimee",
                    "Votre photo de profil a ete supprimee.", true);
            if (TopbarController.getInstance() != null) {
                TopbarController.getInstance().rafraichirAvatar();
            }
        });
        new Thread(task).start();
    }

    private void afficherPhotoDepuisBase64(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            Image img = new Image(new ByteArrayInputStream(bytes));
            org.example.utils.CircularImageUtil.appliquer(avatarImage, img, 90);
            avatarInitiales.setVisible(false);
            avatarInitiales.setManaged(false);
            photoBase64 = base64;
        } catch (Exception e) {
            avatarImage.setVisible(false);
            avatarImage.setManaged(false);
        }
    }

    // ── Modifier email ─────────────────────────────────────────────────────────
    @FXML
    private void handleSauvegarderEmail() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            setErr(errEmail, "L'email est obligatoire.");
            return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            setErr(errEmail, "Format d'email invalide.");
            return;
        }
        clearErr(errEmail);
        btnSauvegarderEmail.setDisable(true);

        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                String sql = "UPDATE utilisateurs SET email=? WHERE id=?";
                try (PreparedStatement ps =
                             DatabaseConfig.getConnection().prepareStatement(sql)) {
                    ps.setString(1, email);
                    ps.setInt(2, u.getId());
                    ps.executeUpdate();
                }
                u.setEmail(email);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            btnSauvegarderEmail.setDisable(false);
            afficherToast("Email mis a jour",
                    "Votre adresse email a ete modifiee.", true);
        });
        task.setOnFailed(e -> {
            btnSauvegarderEmail.setDisable(false);
            String msg = task.getException().getMessage();
            if (msg != null && msg.contains("Duplicate"))
                setErr(errEmail, "Cet email est deja utilise.");
            else
                setErr(errEmail, "Erreur : " + msg);
        });
        new Thread(task).start();
    }

    // ── Modifier mot de passe ──────────────────────────────────────────────────
    @FXML
    private void handleSauvegarderMdp() {
        String ancien   = ancienMdpField.getText();
        String nouveau  = nouveauMdpField.getText();
        String confirm  = confirmMdpField.getText();
        boolean ok = true;

        clearErr(errAncienMdp); clearErr(errNouveauMdp); clearErr(errConfirmMdp);

        if (ancien.isEmpty()) {
            setErr(errAncienMdp, "Saisissez votre mot de passe actuel.");
            ok = false;
        }
        if (nouveau.length() < 8) {
            setErr(errNouveauMdp, "Minimum 8 caracteres.");
            ok = false;
        }
        if (!nouveau.equals(confirm)) {
            setErr(errConfirmMdp, "Les mots de passe ne correspondent pas.");
            ok = false;
        }
        if (!ok) return;

        btnSauvegarderMdp.setDisable(true);
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                // Vérifier l'ancien mot de passe
                Utilisateur check = service.authentifier(u.getEmail(), ancien);
                if (check == null) throw new Exception("ANCIEN_INCORRECT");
                service.modifierMotDePasse(u.getId(), nouveau);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            btnSauvegarderMdp.setDisable(false);
            ancienMdpField.clear();
            nouveauMdpField.clear();
            confirmMdpField.clear();
            afficherToast("Mot de passe mis a jour",
                    "Votre mot de passe a ete modifie.", true);
        });
        task.setOnFailed(e -> {
            btnSauvegarderMdp.setDisable(false);
            String msg = task.getException().getMessage();
            if ("ANCIEN_INCORRECT".equals(msg))
                setErr(errAncienMdp, "Mot de passe actuel incorrect.");
            else
                setErr(errAncienMdp, "Erreur : " + msg);
        });
        new Thread(task).start();
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
        toast.setMaxWidth(300); toast.setMinWidth(240);
        toast.setMaxHeight(40);
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        FontIcon icon = FontIcon.of(ikon, 18, Color.WHITE);
        Label tl = new Label(titre);
        tl.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:white;");
        Region sp = new Region();
        javafx.scene.layout.HBox.setHgrow(sp, Priority.ALWAYS);
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
        javafx.scene.layout.StackPane.setAlignment(toast,
                javafx.geometry.Pos.TOP_RIGHT);
        javafx.scene.layout.StackPane.setMargin(toast,
                new javafx.geometry.Insets(16, 16, 0, 0));
        cp.getChildren().add(toast);
        javafx.animation.TranslateTransition si =
                new javafx.animation.TranslateTransition(
                        javafx.util.Duration.millis(250), toast);
        si.setFromX(320); si.setToX(0);
        javafx.animation.FadeTransition fi =
                new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(200), toast);
        fi.setFromValue(0); fi.setToValue(1);
        si.play(); fi.play();
        Runnable fermer = () -> {
            javafx.animation.FadeTransition fo =
                    new javafx.animation.FadeTransition(
                            javafx.util.Duration.millis(250), toast);
            javafx.animation.TranslateTransition so =
                    new javafx.animation.TranslateTransition(
                            javafx.util.Duration.millis(250), toast);
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

    private StackPane getContentPane() {
        try { return (StackPane) emailField.getScene().lookup("#contentPane"); }
        catch (Exception e) { return null; }
    }

    private void setErr(Label l, String msg) {
        l.setText(msg); l.setVisible(true); l.setManaged(true);
    }
    private void clearErr(Label l) {
        l.setVisible(false); l.setManaged(false);
    }
}
