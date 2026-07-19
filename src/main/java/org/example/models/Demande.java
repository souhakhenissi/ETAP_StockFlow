package org.example.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Demande {

    public enum Statut   { en_attente, approuvee, rejetee, livree, annulee }
    public enum Priorite { normale, haute, urgente }

    private int           id;
    private String        numeroDemande;
    private int           idIntervenant;
    private String        nomIntervenant;
    private String        emailIntervenant;
    private String        siteIntervenant;
    private Statut        statut;
    private Priorite      priorite;
    private String        justification;
    private String        motifRejet;
    private LocalDateTime dateCreation;
    private LocalDateTime dateTraitement;
    private int           idAdminTraitant;
    private List<LigneDemande> lignes = new ArrayList<>();

    public String getStatutLabel() {
        if (statut == null) return "—";
        return switch (statut) {
            case en_attente -> "En attente";
            case approuvee  -> "Approuvee";
            case rejetee    -> "Rejetee";
            case livree     -> "Livree";
            case annulee    -> "Annulee";
        };
    }

    public String getStatutStyle() {
        if (statut == null) return "badge-inactif";
        return switch (statut) {
            case en_attente -> "badge-attente";
            case approuvee  -> "badge-actif";
            case rejetee    -> "badge-bloque";
            case livree     -> "badge-livree";
            case annulee    -> "badge-inactif";
        };
    }

    public String getPrioriteLabel() {
        if (priorite == null) return "Normale";
        return switch (priorite) {
            case normale -> "Normale";
            case haute   -> "Haute";
            case urgente -> "Urgente";
        };
    }

    public String getPrioriteStyle() {
        if (priorite == null) return "badge-priorite-normale";
        return switch (priorite) {
            case normale -> "badge-priorite-normale";
            case haute   -> "badge-priorite-haute";
            case urgente -> "badge-priorite-urgente";
        };
    }

    public String getArticlesResume() {
        if (lignes == null || lignes.isEmpty()) return "—";
        return lignes.stream()
                .map(l -> l.getQuantite() + "x " + l.getNomMateriel())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("—");
    }

    public int getId()                                  { return id; }
    public void setId(int id)                           { this.id = id; }
    public String getNumeroDemande()                    { return numeroDemande; }
    public void setNumeroDemande(String n)              { this.numeroDemande = n; }
    public int getIdIntervenant()                       { return idIntervenant; }
    public void setIdIntervenant(int id)                { this.idIntervenant = id; }
    public String getNomIntervenant()                   { return nomIntervenant; }
    public void setNomIntervenant(String n)             { this.nomIntervenant = n; }
    public String getEmailIntervenant()                 { return emailIntervenant; }
    public void setEmailIntervenant(String e)           { this.emailIntervenant = e; }
    public String getSiteIntervenant()                  { return siteIntervenant; }
    public void setSiteIntervenant(String s)            { this.siteIntervenant = s; }
    public Statut getStatut()                           { return statut; }
    public void setStatut(Statut statut)                { this.statut = statut; }
    public Priorite getPriorite()                       { return priorite; }
    public void setPriorite(Priorite priorite)          { this.priorite = priorite; }
    public String getJustification()                    { return justification; }
    public void setJustification(String j)              { this.justification = j; }
    public String getMotifRejet()                       { return motifRejet; }
    public void setMotifRejet(String m)                 { this.motifRejet = m; }
    public LocalDateTime getDateCreation()              { return dateCreation; }
    public void setDateCreation(LocalDateTime d)        { this.dateCreation = d; }
    public LocalDateTime getDateTraitement()            { return dateTraitement; }
    public void setDateTraitement(LocalDateTime d)      { this.dateTraitement = d; }
    public int getIdAdminTraitant()                     { return idAdminTraitant; }
    public void setIdAdminTraitant(int id)              { this.idAdminTraitant = id; }
    public List<LigneDemande> getLignes()               { return lignes; }
    public void setLignes(List<LigneDemande> l)         { this.lignes = l; }
}
