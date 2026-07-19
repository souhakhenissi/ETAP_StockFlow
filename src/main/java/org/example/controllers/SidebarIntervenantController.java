package org.example.controllers;

import javafx.scene.control.ScrollPane;
import org.example.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Consumer;

public class SidebarIntervenantController {

    @FXML private VBox      sidebarRoot;
    @FXML private VBox navContent;
    @FXML private ScrollPane scrollPane;
    @FXML private ImageView logoFull, logoIcon;
    @FXML private Label     secPrincipal;
    @FXML private Button    toggleBtn;
    @FXML private FontIcon  toggleIcon;
    @FXML private Button    btnDashboard, btnDemandes, btnAffectations, btnProfil;
    @FXML private Button    btnLogout;

    private boolean          collapsed = false;
    private Consumer<String> navigationHandler;
    private Runnable         logoutHandler;
    private List<Button>     allNavButtons;

    @FXML
    public void initialize() {
        allNavButtons = List.of(btnDashboard, btnDemandes, btnAffectations, btnProfil);
        allNavButtons.forEach(b -> b.setAlignment(javafx.geometry.Pos.CENTER_LEFT));
        btnLogout.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        btnLogout.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        btnDashboard.setText("  Tableau de bord");
        btnDemandes.setText("  Mes demandes");
        btnAffectations.setText("  Mes affectations");
        btnProfil.setText("  Mon profil");
        btnLogout.setText("  Deconnexion");
        toggleIcon.setIconLiteral("mdi2m-menu");
    }

    @FXML
    private void handleToggle() {
        collapsed = !collapsed;
        if (collapsed) {
            sidebarRoot.getStyleClass().add("collapsed");
            logoFull.setVisible(false); logoFull.setManaged(false);
            logoIcon.setVisible(true);  logoIcon.setManaged(true);
            secPrincipal.setVisible(false); secPrincipal.setManaged(false);
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
            logoIcon.setVisible(false); logoIcon.setManaged(false);
            logoFull.setVisible(true);  logoFull.setManaged(true);
            secPrincipal.setVisible(true); secPrincipal.setManaged(true);
            allNavButtons.forEach(b -> {
                b.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
                b.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                b.setMaxWidth(Double.MAX_VALUE);
            });
            btnLogout.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            btnLogout.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            btnDashboard.setText("  Tableau de bord");
            btnDemandes.setText("  Mes demandes");
            btnAffectations.setText("  Mes affectations");
            btnProfil.setText("  Mon profil");
            btnLogout.setText("  Deconnexion");
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

    public void setNavigationHandler(Consumer<String> h) { this.navigationHandler = h; }
    public void setLogoutHandler(Runnable h)             { this.logoutHandler = h; }
    public VBox getRoot()                                { return sidebarRoot; }
}
