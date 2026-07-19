package org.example.models;

public class LigneDemande {
    private int    id;
    private int    idDemande;
    private String nomMateriel;
    private int    idMateriel;
    private int    quantite;

    public LigneDemande() {}
    public LigneDemande(String nomMateriel, int idMateriel, int quantite) {
        this.nomMateriel = nomMateriel;
        this.idMateriel  = idMateriel;
        this.quantite    = quantite;
    }

    public int    getId()                  { return id; }
    public void   setId(int id)            { this.id = id; }
    public int    getIdDemande()           { return idDemande; }
    public void   setIdDemande(int id)     { this.idDemande = id; }
    public String getNomMateriel()         { return nomMateriel; }
    public void   setNomMateriel(String n) { this.nomMateriel = n; }
    public int    getIdMateriel()          { return idMateriel; }
    public void   setIdMateriel(int id)    { this.idMateriel = id; }
    public int    getQuantite()            { return quantite; }
    public void   setQuantite(int q)       { this.quantite = q; }
}
