package org.example.utils;

import org.example.models.Utilisateur;

/**
 * Singleton léger qui conserve l'utilisateur connecté en mémoire
 * pour toute la durée de la session JavaFX.
 */
public class SessionManager {

    private static SessionManager instance;
    private Utilisateur utilisateurConnecte;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void setUtilisateurConnecte(Utilisateur u) {
        this.utilisateurConnecte = u;
    }

    public Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public boolean isAdmin() {
        return utilisateurConnecte != null
                && utilisateurConnecte.getRole() == Utilisateur.Role.admin;
    }

    public void logout() {
        utilisateurConnecte = null;
    }
}

