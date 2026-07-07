package org.example.models;

import java.time.LocalDateTime;

public class MouvementStock {

    public enum Type { entree, sortie }

    private int           id;
    private String        numeroBon;
    private Type          type;
    private int           idMateriel;
    private String        nomMateriel;
    private String        referenceMateriel;
    private int           quantite;
    private double        prixUnitaire;
    private int           idFournisseur;
    private String        nomFournisseur;
    private String        numeroDemande;
    private String        siteEtap;
    private String        observation;
    private LocalDateTime dateCreation;

    public MouvementStock() {}

    public double getMontantTotal() { return quantite * prixUnitaire; }
    public String getTypeLabel()    { return type == Type.entree ? "Entree" : "Sortie"; }

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public String getNumeroBon()                    { return numeroBon; }
    public void setNumeroBon(String n)              { this.numeroBon = n; }
    public Type getType()                           { return type; }
    public void setType(Type type)                  { this.type = type; }
    public int getIdMateriel()                      { return idMateriel; }
    public void setIdMateriel(int id)               { this.idMateriel = id; }
    public String getNomMateriel()                  { return nomMateriel; }
    public void setNomMateriel(String n)            { this.nomMateriel = n; }
    public String getReferenceMateriel()            { return referenceMateriel; }
    public void setReferenceMateriel(String r)      { this.referenceMateriel = r; }
    public int getQuantite()                        { return quantite; }
    public void setQuantite(int q)                  { this.quantite = q; }
    public double getPrixUnitaire()                 { return prixUnitaire; }
    public void setPrixUnitaire(double p)           { this.prixUnitaire = p; }
    public int getIdFournisseur()                   { return idFournisseur; }
    public void setIdFournisseur(int id)            { this.idFournisseur = id; }
    public String getNomFournisseur()               { return nomFournisseur; }
    public void setNomFournisseur(String n)         { this.nomFournisseur = n; }
    public String getNumeroDemande()                { return numeroDemande; }
    public void setNumeroDemande(String n)          { this.numeroDemande = n; }
    public String getSiteEtap()                     { return siteEtap; }
    public void setSiteEtap(String s)               { this.siteEtap = s; }
    public String getObservation()                  { return observation; }
    public void setObservation(String o)            { this.observation = o; }
    public LocalDateTime getDateCreation()          { return dateCreation; }
    public void setDateCreation(LocalDateTime d)    { this.dateCreation = d; }
}
