package org.example.models;

import java.time.LocalDate;

public class Materiel {

    public enum Etat { neuf, en_service, en_panne, reforme }

    private int       id;
    private String    nom;
    private String    reference;
    private String    categorie;
    private String    marque;
    private String    modele;
    private String    numeroSerie;
    private Etat      etat;
    private int       quantiteStock;
    private int       seuilAlerte;
    private LocalDate dateAcquisition;
    private int       idFournisseur;
    private String    nomFournisseur; // jointure affichage

    public Materiel() {}

    // ── Helpers ────────────────────────────────────────────────────────────────

    public boolean estEnAlerte() {
        return quantiteStock <= seuilAlerte;
    }

    public String getEtatLabel() {
        if (etat == null) return "—";
        return switch (etat) {
            case neuf       -> "Neuf";
            case en_service -> "En service";
            case en_panne   -> "En panne";
            case reforme    -> "Réformé";
        };
    }

    public String getEtatStyle() {
        if (etat == null) return "badge-inactif";
        return switch (etat) {
            case neuf       -> "badge-neuf";
            case en_service -> "badge-actif";
            case en_panne   -> "badge-bloque";
            case reforme    -> "badge-inactif";
        };
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getNom()                          { return nom; }
    public void setNom(String nom)                  { this.nom = nom; }

    public String getReference()                    { return reference; }
    public void setReference(String reference)      { this.reference = reference; }

    public String getCategorie()                    { return categorie; }
    public void setCategorie(String categorie)      { this.categorie = categorie; }

    public String getMarque()                       { return marque; }
    public void setMarque(String marque)            { this.marque = marque; }

    public String getModele()                       { return modele; }
    public void setModele(String modele)            { this.modele = modele; }

    public String getNumeroSerie()                  { return numeroSerie; }
    public void setNumeroSerie(String n)            { this.numeroSerie = n; }

    public Etat getEtat()                           { return etat; }
    public void setEtat(Etat etat)                  { this.etat = etat; }

    public int getQuantiteStock()                   { return quantiteStock; }
    public void setQuantiteStock(int q)             { this.quantiteStock = q; }

    public int getSeuilAlerte()                     { return seuilAlerte; }
    public void setSeuilAlerte(int s)               { this.seuilAlerte = s; }

    public LocalDate getDateAcquisition()           { return dateAcquisition; }
    public void setDateAcquisition(LocalDate d)     { this.dateAcquisition = d; }

    public int getIdFournisseur()                   { return idFournisseur; }
    public void setIdFournisseur(int id)            { this.idFournisseur = id; }

    public String getNomFournisseur()               { return nomFournisseur; }
    public void setNomFournisseur(String n)         { this.nomFournisseur = n; }
}
