package org.example.controllers;

import org.example.models.Utilisateur;
import org.example.services.FaceRecognitionService;
import org.example.services.UtilisateurService;
import org.example.utils.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Rect;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contrôleur de la vue de connexion par reconnaissance faciale.
 *
 * Vue FXML : face_login.fxml
 * Flux :
 *  1. Ouverture webcam
 *  2. Affichage flux vidéo temps réel + rectangle de détection
 *  3. Clic "S'identifier" → comparaison avec les visages en DB
 *  4. Résultat → connexion ou message d'erreur
 */
public class FaceLoginController {

    @FXML private ImageView     cameraView;
    @FXML private Label         statusLabel;
    @FXML private Button        btnIdentifier;
    @FXML private Button        btnAnnuler;
    @FXML private ProgressBar   detectionBar;
    @FXML private Label         instructionLabel;

    private OpenCVFrameGrabber       grabber;
    private Java2DFrameConverter     converter;
    private FaceRecognitionService   faceService;
    private UtilisateurService       utilisateurService;

    private Thread                   cameraThread;
    private final AtomicBoolean      cameraActive = new AtomicBoolean(false);
    private final AtomicBoolean      enCoursDAnalyse = new AtomicBoolean(false);

    // Frame courante partagée entre le thread caméra et l'analyse
    private volatile BufferedImage   frameCourant;

    // ── Initialisation ─────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        faceService        = new FaceRecognitionService();
        utilisateurService = new UtilisateurService();
        converter          = new Java2DFrameConverter();

        btnIdentifier.setDisable(true);
        detectionBar.setProgress(0);
        statusLabel.setText("Initialisation de la caméra…");
        instructionLabel.setText("Placez votre visage face à la caméra");

        // Init OpenCV + webcam dans un thread séparé
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                faceService.initialiser();
                demarrerWebcam();
                return null;
            }
        };
        initTask.setOnSucceeded(e -> {
            btnIdentifier.setDisable(false);
            statusLabel.setText("Caméra prête. Cliquez sur « S'identifier ».");
        });
        initTask.setOnFailed(e -> {
            statusLabel.setText("❌ " + initTask.getException().getMessage());
            btnIdentifier.setDisable(true);
        });
        new Thread(initTask).start();
    }

    // ── Webcam ─────────────────────────────────────────────────────────────────

    private void demarrerWebcam() throws FrameGrabber.Exception {
        grabber = new OpenCVFrameGrabber(0); // 0 = première webcam
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        grabber.start();

        cameraActive.set(true);

        cameraThread = new Thread(() -> {
            while (cameraActive.get()) {
                try {
                    Frame frame = grabber.grab();
                    if (frame == null) continue;

                    BufferedImage image = converter.convert(frame);
                    if (image == null) continue;

                    frameCourant = image;

                    // Détection en temps réel (légère — juste le rectangle)
                    Rect rect = faceService.detecterRectangleVisage(image);
                    BufferedImage affichage = (rect != null)
                            ? faceService.dessinerCadreVisage(image, rect, false)
                            : image;

                    // Mise à jour du ImageView (sur FX thread)
                    WritableImage fxImage = SwingFXUtils.toFXImage(affichage, null);
                    Platform.runLater(() -> {
                        cameraView.setImage(fxImage);
                        // Indicateur de présence de visage
                        if (!enCoursDAnalyse.get()) {
                            detectionBar.setProgress(rect != null ? 1.0 : 0.0);
                            statusLabel.setText(rect != null
                                    ? "✅ Visage détecté — Prêt à s'identifier"
                                    : "🔍 Aucun visage détecté");
                        }
                    });

                    Thread.sleep(33); // ~30 fps

                } catch (Exception e) {
                    if (cameraActive.get()) {
                        Platform.runLater(() ->
                                statusLabel.setText("Erreur caméra : " + e.getMessage()));
                    }
                    break;
                }
            }
        }, "Camera-Thread");

        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    // ── Identification ─────────────────────────────────────────────────────────

    @FXML
    private void handleIdentifier() {
        if (frameCourant == null) {
            statusLabel.setText("❌ Aucune image disponible.");
            return;
        }
        if (enCoursDAnalyse.get()) return;

        enCoursDAnalyse.set(true);
        btnIdentifier.setDisable(true);
        statusLabel.setText("🔄 Analyse en cours…");
        detectionBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        BufferedImage frameAnalyse = frameCourant; // copie de référence

        Task<Utilisateur> task = new Task<>() {
            @Override
            protected Utilisateur call() throws Exception {

                // 1. Détecter le visage dans le frame courant
                org.bytedeco.opencv.opencv_core.Mat visage =
                        faceService.detecterVisage(frameAnalyse);
                if (visage == null) throw new Exception("AUCUN_VISAGE");

                // 2. Charger les utilisateurs avec encodage facial
                List<Utilisateur> usersAvecVisage =
                        utilisateurService.findAvecFaceEncoding();
                if (usersAvecVisage.isEmpty()) throw new Exception("AUCUN_ENCODAGE");

                // 3. Entraîner le modèle LBPH
                boolean ok = faceService.entrainerModele(usersAvecVisage);
                if (!ok) throw new Exception("ENTRAINEMENT_ECHOUE");

                // 4. Reconnaissance
                FaceRecognitionService.ResultatReconnaissance resultat =
                        faceService.reconnaitre(visage);

                if (!resultat.reconnu()) throw new Exception("NON_RECONNU");

                // 5. Charger l'utilisateur correspondant
                Utilisateur u = utilisateurService.findById(resultat.idUtilisateur());
                if (u == null) throw new Exception("UTILISATEUR_INTROUVABLE");
                if (u.getStatut() == Utilisateur.Statut.bloque)
                    throw new Exception("COMPTE_BLOQUE");

                return u;
            }
        };

        task.setOnSucceeded(e -> {
            enCoursDAnalyse.set(false);
            Utilisateur u = task.getValue();

            // Cadre vert sur le visage reconnu
            Rect rect = faceService.detecterRectangleVisage(frameAnalyse);
            if (rect != null) {
                BufferedImage affichage =
                        faceService.dessinerCadreVisage(frameAnalyse, rect, true);
                cameraView.setImage(SwingFXUtils.toFXImage(affichage, null));
            }

            statusLabel.setText("✅ Bienvenue, " + u.getNomComplet() + " !");
            detectionBar.setProgress(1.0);

            // Laisser 1 seconde pour voir le message puis ouvrir le dashboard
            new Thread(() -> {
                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    arreterWebcam();
                    SessionManager.getInstance().setUtilisateurConnecte(u);
                    ouvrirDashboard(u);
                });
            }).start();
        });

        task.setOnFailed(e -> {
            enCoursDAnalyse.set(false);
            btnIdentifier.setDisable(false);
            detectionBar.setProgress(0);

            String msg = task.getException().getMessage();
            statusLabel.setText(switch (msg) {
                case "AUCUN_VISAGE"          -> "❌ Aucun visage détecté. Réessayez.";
                case "AUCUN_ENCODAGE"        -> "❌ Aucun visage enregistré en base.";
                case "ENTRAINEMENT_ECHOUE"   -> "❌ Erreur d'initialisation du modèle.";
                case "NON_RECONNU"           -> "❌ Visage non reconnu. Utilisez l'identifiant.";
                case "COMPTE_BLOQUE"         -> "❌ Compte bloqué. Contactez l'administrateur.";
                case "UTILISATEUR_INTROUVABLE"-> "❌ Utilisateur introuvable.";
                default                      -> "❌ Erreur : " + msg;
            });
        });

        new Thread(task).start();
    }

    // ── Enregistrement d'un visage (appelé depuis la gestion utilisateurs) ─────

    /**
     * Lance une session d'enregistrement facial pour un utilisateur existant.
     * Capture 20 frames sur 3 secondes et stocke l'encodage en DB.
     *
     * @param idUtilisateur l'id de l'utilisateur à enregistrer
     * @param onComplete    callback une fois l'enregistrement terminé
     */
    public void enregistrerVisage(int idUtilisateur, Runnable onComplete) {
        if (frameCourant == null) {
            statusLabel.setText("❌ Caméra non disponible.");
            return;
        }

        btnIdentifier.setDisable(true);
        instructionLabel.setText("📸 Regardez la caméra — Enregistrement en cours…");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<BufferedImage> frames = new java.util.ArrayList<>();

                // Capturer 20 frames sur ~2 secondes
                for (int i = 0; i < 20; i++) {
                    if (frameCourant != null) frames.add(frameCourant);
                    Thread.sleep(100);
                    int finalI = i;
                    Platform.runLater(() ->
                            detectionBar.setProgress((finalI + 1) / 20.0));
                }

                String encoding = faceService.encoderDepuisFrames(frames);
                if (encoding == null) throw new Exception("Aucun visage détecté pendant la capture.");

                new UtilisateurService().enregistrerFaceEncoding(idUtilisateur, encoding);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("✅ Visage enregistré avec succès !");
                instructionLabel.setText("Placez votre visage face à la caméra");
                btnIdentifier.setDisable(false);
                if (onComplete != null) onComplete.run();
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("❌ " + task.getException().getMessage());
                btnIdentifier.setDisable(false);
            });
        });

        new Thread(task).start();
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    private void ouvrirDashboard(Utilisateur u) {
        try {
            String fxml = u.isAdmin()
                    ? "/views/admin/dashboard.fxml"
                    : "/views/intervenant/dashboard.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/styles/main.css").toExternalForm());
            Stage stage = (Stage) btnAnnuler.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (IOException ex) {
            statusLabel.setText("Erreur chargement tableau de bord.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleAnnuler() {
        arreterWebcam();
        // Retour vers login
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/login.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/styles/main.css").toExternalForm());
            Stage stage = (Stage) btnAnnuler.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // ── Nettoyage ──────────────────────────────────────────────────────────────

    private void arreterWebcam() {
        cameraActive.set(false);
        if (cameraThread != null) cameraThread.interrupt();
        if (grabber != null) {
            try { grabber.stop(); grabber.release(); }
            catch (FrameGrabber.Exception ignored) {}
        }
    }

    /** Appelé automatiquement quand la fenêtre est fermée */
    public void shutdown() {
        arreterWebcam();
    }
}

