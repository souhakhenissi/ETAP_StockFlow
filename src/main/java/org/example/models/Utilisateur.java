package org.example.models;

import java.time.LocalDateTime;

public class Utilisateur {

    public enum Role   { admin, intervenant }
    public enum Statut { actif, inactif, bloque }

    private int           id;
    private String        nom;
    private String        prenom;
    private String        email;
    private String        motDePasse;
    private Role          role;
    private String        siteAffecte;
    private Statut        statut;
    private String        photo;
    private String        faceEncoding;
    private String        resetToken;
    private LocalDateTime resetExpiry;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    public Utilisateur() {}

    public Utilisateur(String nom, String prenom, String email,
                       String motDePasse, Role role, String siteAffecte) {
        this.nom         = nom;
        this.prenom      = prenom;
        this.email       = email;
        this.motDePasse  = motDePasse;
        this.role        = role;
        this.siteAffecte = siteAffecte;
        this.statut      = Statut.actif;
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public boolean isAdmin() {
        return role == Role.admin;
    }

    public boolean isActif() {
        return statut == Statut.actif;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getNom()                          { return nom; }
    public void setNom(String nom)                  { this.nom = nom; }

    public String getPrenom()                       { return prenom; }
    public void setPrenom(String prenom)            { this.prenom = prenom; }

    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }

    public String getMotDePasse()                   { return motDePasse; }
    public void setMotDePasse(String motDePasse)    { this.motDePasse = motDePasse; }

    public Role getRole()                           { return role; }
    public void setRole(Role role)                  { this.role = role; }

    public String getSiteAffecte()                  { return siteAffecte; }
    public void setSiteAffecte(String s)            { this.siteAffecte = s; }

    public Statut getStatut()                       { return statut; }
    public void setStatut(Statut statut)            { this.statut = statut; }

    public String getPhoto()                        { return photo; }
    public void setPhoto(String photo)              { this.photo = photo; }

    public String getFaceEncoding()                 { return faceEncoding; }
    public void setFaceEncoding(String fe)          { this.faceEncoding = fe; }

    public String getResetToken()                   { return resetToken; }
    public void setResetToken(String t)             { this.resetToken = t; }

    public LocalDateTime getResetExpiry()           { return resetExpiry; }
    public void setResetExpiry(LocalDateTime e)     { this.resetExpiry = e; }

    public LocalDateTime getDateCreation()          { return dateCreation; }
    public void setDateCreation(LocalDateTime d)    { this.dateCreation = d; }

    public LocalDateTime getDateModification()      { return dateModification; }
    public void setDateModification(LocalDateTime d){ this.dateModification = d; }
}

