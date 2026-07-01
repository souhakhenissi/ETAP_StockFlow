package org.example.controllers;

import javafx.scene.control.ScrollPane;
import org.example.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Consumer;

public class SidebarController {

    @FXML private VBox      sidebarRoot;
    @FXML private VBox navContent;
    @FXML private ScrollPane scrollPane;
    @FXML private HBox      logoBox;
    @FXML private ImageView logoFull;
    @FXML private ImageView logoIcon;
    @FXML private Label     secPrincipal, secAdmin, secCompte;
    @FXML private Button    toggleBtn;
    @FXML private FontIcon  toggleIcon;
    @FXML private Button    btnDashboard, btnMateriels, btnStock, btnDemandes;
    @FXML private Button    btnAffectations, btnFournisseurs, btnRecherche;
    @FXML private Button    btnRapports, btnUtilisateurs;
    @FXML private Button    btnProfil, btnParametres, btnLogout;

    private boolean collapsed = false;
    private Consumer<String> navigationHandler;
    private Runnable         logoutHandler;
    private List<Button>     allNavButtons;

    @FXML
    public void initialize() {
        allNavButtons = List.of(
                btnDashboard, btnMateriels, btnStock, btnDemandes,
                btnAffectations, btnFournisseurs, btnRecherche,
                btnRapports, btnUtilisateurs, btnProfil, btnParametres
        );

        allNavButtons.forEach(b -> {
            b.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            b.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            b.setMaxWidth(Double.MAX_VALUE);
        });
        btnLogout.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        btnLogout.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Réappliquer les libellés (perdus si jamais remplacés ailleurs)
        btnDashboard.setText("  Tableau de bord");
        btnMateriels.setText("  Matériels");
        btnStock.setText("  Stock");
        btnDemandes.setText("  Demandes");
        btnAffectations.setText("  Affectations");
        btnFournisseurs.setText("  Fournisseurs");
        btnRecherche.setText("  Recherche");
        btnRapports.setText("  Rapports");
        btnUtilisateurs.setText("  Utilisateurs");
        btnProfil.setText("  Profil");
        btnParametres.setText("  Paramètres");
        btnLogout.setText("  Déconnexion");

        toggleIcon.setIconLiteral("mdi2m-menu");

        boolean isAdmin = SessionManager.getInstance().isAdmin();
        secAdmin.setVisible(isAdmin);       secAdmin.setManaged(isAdmin);
        btnRapports.setVisible(isAdmin);    btnRapports.setManaged(isAdmin);
        btnUtilisateurs.setVisible(isAdmin);btnUtilisateurs.setManaged(isAdmin);

        // S'assurer que chaque bouton garde son icône mais peut perdre son label
        allNavButtons.forEach(b -> b.setAlignment(javafx.geometry.Pos.CENTER_LEFT));

        scrollPane.requestLayout();
    }

    @FXML
    private void handleToggle() {
        collapsed = !collapsed;

        if (collapsed) {
            sidebarRoot.getStyleClass().add("collapsed");

            // Swap logo : complet → icône seule
            logoFull.setVisible(false); logoFull.setManaged(false);
            logoIcon.setVisible(true);  logoIcon.setManaged(true);

            // Masquer les sections (labels) — gêneraient en mode icône
            secPrincipal.setVisible(false); secPrincipal.setManaged(false);
            secAdmin.setVisible(false);     secAdmin.setManaged(false);
            secCompte.setVisible(false);    secCompte.setManaged(false);

            // Boutons : ne garder que l'icône, centrer
            allNavButtons.forEach(b -> {
                b.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                b.setAlignment(javafx.geometry.Pos.CENTER);
                b.setMaxWidth(48);
            });
            btnLogout.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            btnLogout.setAlignment(javafx.geometry.Pos.CENTER);

            toggleIcon.setIconLiteral("mdi2c-chevron-right");

            sidebarRoot.requestLayout();
            navContent.requestLayout();
            scrollPane.requestLayout();

        } else {
            sidebarRoot.getStyleClass().remove("collapsed");

            // Swap logo : icône seule → complet
            logoIcon.setVisible(false); logoIcon.setManaged(false);
            logoFull.setVisible(true);  logoFull.setManaged(true);

            secPrincipal.setVisible(true); secPrincipal.setManaged(true);
            boolean isAdmin = SessionManager.getInstance().isAdmin();
            secAdmin.setVisible(isAdmin);  secAdmin.setManaged(isAdmin);
            secCompte.setVisible(true);    secCompte.setManaged(true);

            // Restaurer icône + texte
            allNavButtons.forEach(b -> {
                b.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
                b.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                b.setMaxWidth(Double.MAX_VALUE);
            });
            btnLogout.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            btnLogout.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Réappliquer les libellés (perdus si jamais remplacés ailleurs)
            btnDashboard.setText("  Tableau de bord");
            btnMateriels.setText("  Matériels");
            btnStock.setText("  Stock");
            btnDemandes.setText("  Demandes");
            btnAffectations.setText("  Affectations");
            btnFournisseurs.setText("  Fournisseurs");
            btnRecherche.setText("  Recherche");
            btnRapports.setText("  Rapports");
            btnUtilisateurs.setText("  Utilisateurs");
            btnProfil.setText("  Profil");
            btnParametres.setText("  Paramètres");
            btnLogout.setText("  Déconnexion");

            toggleIcon.setIconLiteral("mdi2m-menu");

            sidebarRoot.requestLayout();
            navContent.requestLayout();
            scrollPane.requestLayout();

        }
    }

    @FXML
    private void nav(javafx.event.ActionEvent e) {
        allNavButtons.forEach(b -> b.getStyleClass().remove("active"));
        Button clicked = (Button) e.getSource();
        clicked.getStyleClass().add("active");
        if (navigationHandler != null) navigationHandler.accept(clicked.getId());
    }

    @FXML
    private void handleLogout() {
        if (logoutHandler != null) logoutHandler.run();
    }

    public void setActiveButton(String buttonId) {
        allNavButtons.forEach(b -> b.getStyleClass().remove("active"));
        allNavButtons.stream()
                .filter(b -> buttonId.equals(b.getId()))
                .findFirst()
                .ifPresent(b -> b.getStyleClass().add("active"));
    }

    public void setNavigationHandler(Consumer<String> handler) { this.navigationHandler = handler; }
    public void setLogoutHandler(Runnable handler)             { this.logoutHandler = handler; }
    public VBox getRoot()                                       { return sidebarRoot; }
}