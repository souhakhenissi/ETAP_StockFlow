package org.example.models;

import java.time.LocalDateTime;

public class Fournisseur {

    private int           id;
    private String        nom;
    private String        email;
    private String        telephone;
    private String        adresse;
    private String        specialite;
    private LocalDateTime dateAjout;

    public Fournisseur() {}

    public Fournisseur(String nom, String email, String telephone,
                       String adresse, String specialite) {
        this.nom        = nom;
        this.email      = email;
        this.telephone  = telephone;
        this.adresse    = adresse;
        this.specialite = specialite;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }

    public String getNom()                       { return nom; }
    public void setNom(String nom)               { this.nom = nom; }

    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }

    public String getTelephone()                 { return telephone; }
    public void setTelephone(String telephone)   { this.telephone = telephone; }

    public String getAdresse()                   { return adresse; }
    public void setAdresse(String adresse)       { this.adresse = adresse; }

    public String getSpecialite()                { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public LocalDateTime getDateAjout()          { return dateAjout; }
    public void setDateAjout(LocalDateTime d)    { this.dateAjout = d; }

    /** Initiales pour l'avatar (2 premières lettres du nom) */
    public String getInitiales() {
        if (nom == null || nom.isBlank()) return "?";
        String[] parts = nom.trim().split("\\s+");
        if (parts.length >= 2)
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        return nom.substring(0, Math.min(2, nom.length())).toUpperCase();
    }
}
