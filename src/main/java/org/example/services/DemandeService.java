package org.example.services;

import org.example.config.DatabaseConfig;
import org.example.models.Demande;
import org.example.models.LigneDemande;
import org.example.utils.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DemandeService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Génération numéro demande ──────────────────────────────────────────────
    public String genererNumeroDemande() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 6);
        return "DEM-" + date + "-" + uuid;
    }

    // ── Lire toutes les demandes (admin) ───────────────────────────────────────
    public List<Demande> findAll() throws SQLException {
        String sql = """
            SELECT d.*,
                   u.nom AS nom_inter, u.prenom AS prenom_inter,
                   u.email AS email_inter, u.site_affecte AS site_inter
            FROM   demandes d
            JOIN   utilisateurs u ON d.id_intervenant = u.id
            ORDER  BY d.date_creation DESC
            """;
        return execQuery(sql, -1);
    }

    // ── Lire les demandes d'un intervenant ─────────────────────────────────────
    public List<Demande> findByIntervenant(int idIntervenant) throws SQLException {
        String sql = """
            SELECT d.*,
                   u.nom AS nom_inter, u.prenom AS prenom_inter,
                   u.email AS email_inter, u.site_affecte AS site_inter
            FROM   demandes d
            JOIN   utilisateurs u ON d.id_intervenant = u.id
            WHERE  d.id_intervenant = ?
            ORDER  BY d.date_creation DESC
            """;
        return execQuery(sql, idIntervenant);
    }

    // ── Stats pour admin ───────────────────────────────────────────────────────
    public int countTotal()      throws SQLException { return countWhere(null); }
    public int countEnAttente()  throws SQLException { return countWhere("statut='en_attente'"); }
    public int countApprouvees() throws SQLException { return countWhere("statut='approuvee'"); }
    public int countRejetees()   throws SQLException { return countWhere("statut='rejetee'"); }
    public int countLivrees()    throws SQLException { return countWhere("statut='livree'"); }

    private int countWhere(String where) throws SQLException {
        String sql = "SELECT COUNT(*) FROM demandes"
                + (where != null ? " WHERE " + where : "");
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Matériels disponibles ──────────────────────────────────────────────────
    public List<String[]> findMateriels() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT id, nom, reference FROM materiels ORDER BY nom";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("nom"),
                        rs.getString("reference")
                });
        }
        return list;
    }

    // ── Emails des admins ──────────────────────────────────────────────────────
    public List<String[]> findAdmins() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = """
            SELECT nom, prenom, email FROM utilisateurs
            WHERE role='admin' AND statut='actif'
            """;
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(new String[]{
                        rs.getString("prenom") + " " + rs.getString("nom"),
                        rs.getString("email")
                });
        }
        return list;
    }

    // ── Créer une demande ──────────────────────────────────────────────────────
    public void creer(Demande d) throws SQLException {
        String sql = """
            INSERT INTO demandes
              (numero_demande, id_intervenant, statut, priorite, justification)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getNumeroDemande());
            ps.setInt(2, d.getIdIntervenant());
            ps.setString(3, Demande.Statut.en_attente.name());
            ps.setString(4, d.getPriorite() != null
                    ? d.getPriorite().name() : Demande.Priorite.normale.name());
            ps.setString(5, d.getJustification());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) d.setId(keys.getInt(1));
        }

        // Insérer les lignes
        for (LigneDemande l : d.getLignes()) {
            insererLigne(d.getId(), l);
        }
    }

    private void insererLigne(int idDemande, LigneDemande l) throws SQLException {
        String sql = """
            INSERT INTO demandes_lignes (id_demande, nom_materiel, id_materiel, quantite)
            VALUES (?,?,?,?)
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            ps.setString(2, l.getNomMateriel());
            if (l.getIdMateriel() > 0) ps.setInt(3, l.getIdMateriel());
            else                       ps.setNull(3, Types.INTEGER);
            ps.setInt(4, l.getQuantite());
            ps.executeUpdate();
        }
    }

    // ── Annuler (intervenant) ──────────────────────────────────────────────────
    public void annuler(int idDemande) throws SQLException {
        changerStatut(idDemande, Demande.Statut.annulee, null, 0);
    }

    // ── Approuver (admin) ──────────────────────────────────────────────────────
    public void approuver(int idDemande, int idAdmin) throws SQLException {
        changerStatut(idDemande, Demande.Statut.approuvee, null, idAdmin);
        // Alimenter demandes_en_attente pour le module Stock
        Demande d = findById(idDemande);
        if (d != null) {
            for (LigneDemande l : d.getLignes()) {
                if (l.getIdMateriel() > 0) {
                    String sql = """
                        INSERT IGNORE INTO demandes_en_attente
                          (numero_demande, id_intervenant, id_materiel, quantite)
                        VALUES (?,?,?,?)
                        """;
                    try (PreparedStatement ps =
                                 DatabaseConfig.getConnection().prepareStatement(sql)) {
                        ps.setString(1, d.getNumeroDemande());
                        ps.setInt(2, d.getIdIntervenant());
                        ps.setInt(3, l.getIdMateriel());
                        ps.setInt(4, l.getQuantite());
                        ps.executeUpdate();
                    }
                    break; // une seule entrée par demande dans demandes_en_attente
                }
            }
        }
    }

    // ── Rejeter (admin) ────────────────────────────────────────────────────────
    public void rejeter(int idDemande, int idAdmin, String motif) throws SQLException {
        changerStatut(idDemande, Demande.Statut.rejetee, motif, idAdmin);
    }

    // ── Marquer livrée (admin) ─────────────────────────────────────────────────
    public void marquerLivree(int idDemande, int idAdmin) throws SQLException {
        changerStatut(idDemande, Demande.Statut.livree, null, idAdmin);
    }

    private void changerStatut(int idDemande, Demande.Statut statut,
                               String motif, int idAdmin) throws SQLException {
        String sql = """
            UPDATE demandes
            SET statut=?, motif_rejet=?, date_traitement=NOW(),
                id_admin_traitant=?
            WHERE id=?
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setString(2, motif);
            if (idAdmin > 0) ps.setInt(3, idAdmin);
            else             ps.setNull(3, Types.INTEGER);
            ps.setInt(4, idDemande);
            ps.executeUpdate();
        }
    }

    // ── Lire une demande par id ────────────────────────────────────────────────
    public Demande findById(int id) throws SQLException {
        String sql = """
            SELECT d.*,
                   u.nom AS nom_inter, u.prenom AS prenom_inter,
                   u.email AS email_inter, u.site_affecte AS site_inter
            FROM   demandes d
            JOIN   utilisateurs u ON d.id_intervenant = u.id
            WHERE  d.id = ?
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Demande d = mapRow(rs);
                d.setLignes(findLignes(d.getId()));
                return d;
            }
        }
        return null;
    }

    // ── Lignes d'une demande ───────────────────────────────────────────────────
    private List<LigneDemande> findLignes(int idDemande) throws SQLException {
        List<LigneDemande> list = new ArrayList<>();
        String sql = "SELECT * FROM demandes_lignes WHERE id_demande=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LigneDemande l = new LigneDemande();
                l.setId(rs.getInt("id"));
                l.setIdDemande(rs.getInt("id_demande"));
                l.setNomMateriel(rs.getString("nom_materiel"));
                l.setIdMateriel(rs.getInt("id_materiel"));
                l.setQuantite(rs.getInt("quantite"));
                list.add(l);
            }
        }
        return list;
    }

    // ── Notifications in-app ───────────────────────────────────────────────────
    public void envoyerNotification(int idDestinataire, String titre,
                                    String message) throws SQLException {
        String sql = """
            INSERT INTO notifications (id_destinataire, titre, message, type)
            VALUES (?,?,?,'demande')
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idDestinataire);
            ps.setString(2, titre);
            ps.setString(3, message);
            ps.executeUpdate();
        }
    }

    public int countNotifsNonLues(int idUser) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE id_destinataire=? AND lue=0";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void marquerNotifsLues(int idUser) throws SQLException {
        String sql = "UPDATE notifications SET lue=1 WHERE id_destinataire=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.executeUpdate();
        }
    }

    // ── Email ──────────────────────────────────────────────────────────────────
    public void envoyerEmailNouvelledemande(Demande d,
                                            List<String[]> admins,
                                            byte[] pdfBytes) {
        String sujet = "Nouvelle demande " + d.getNumeroDemande()
                + " de " + d.getNomIntervenant();

        String articles = d.getLignes().stream()
                .map(l -> "<li>" + l.getQuantite() + "× " + l.getNomMateriel() + "</li>")
                .reduce("", String::concat);

        String corps = """
            <html><body style='font-family:Arial;color:#1a1a2e;'>
              <h2 style='color:#214293;'>ETAP StockFlow — Nouvelle demande</h2>
              <p>Une nouvelle demande a été soumise par <strong>%s</strong>
                 (%s).</p>
              <table style='border-collapse:collapse;width:100%%;'>
                <tr><td style='padding:6px;background:#e8f0fb;font-weight:bold;'>N° Demande</td>
                    <td style='padding:6px;'>%s</td></tr>
                <tr><td style='padding:6px;background:#e8f0fb;font-weight:bold;'>Date</td>
                    <td style='padding:6px;'>%s</td></tr>
                <tr><td style='padding:6px;background:#e8f0fb;font-weight:bold;'>Priorité</td>
                    <td style='padding:6px;'>%s</td></tr>
                <tr><td style='padding:6px;background:#e8f0fb;font-weight:bold;'>Site</td>
                    <td style='padding:6px;'>%s</td></tr>
                <tr><td style='padding:6px;background:#e8f0fb;font-weight:bold;'>Justification</td>
                    <td style='padding:6px;'>%s</td></tr>
              </table>
              <h3 style='color:#214293;'>Articles demandés :</h3>
              <ul>%s</ul>
              <p style='color:#6b7280;'>Connectez-vous à ETAP StockFlow pour traiter cette demande.</p>
            </body></html>
            """.formatted(
                d.getNomIntervenant(), d.getSiteIntervenant(),
                d.getNumeroDemande(),
                d.getDateCreation() != null ? d.getDateCreation().format(FMT) : "—",
                d.getPrioriteLabel(),
                d.getSiteIntervenant() != null ? d.getSiteIntervenant() : "—",
                d.getJustification() != null ? d.getJustification() : "—",
                articles
        );

        for (String[] admin : admins) {
            try {
                PasswordUtil.envoyerEmailAvecPdf(
                        admin[1], sujet, corps,
                        pdfBytes, d.getNumeroDemande() + ".pdf");
            } catch (Exception e) {
                System.err.println("[Email] Erreur envoi admin "
                        + admin[1] + " : " + e.getMessage());
            }
        }
    }

    public void envoyerEmailStatutChange(Demande d, String statut,
                                         String motif) {
        String sujet = "Votre demande " + d.getNumeroDemande()
                + " — " + statut;

        String motifHtml = (motif != null && !motif.isBlank())
                ? "<p><strong>Motif :</strong> " + motif + "</p>"
                : "<p>Si vous souhaitez plus d'informations, "
                  + "contactez l'administrateur.</p>";

        String couleur = switch (statut) {
            case "Approuvée" -> "#16a34a";
            case "Rejetée"   -> "#dc2626";
            case "Livrée"    -> "#0154a6";
            default          -> "#214293";
        };

        String corps = """
            <html><body style='font-family:Arial;color:#1a1a2e;'>
              <h2 style='color:%s;'>ETAP StockFlow — Demande %s</h2>
              <p>Votre demande <strong>%s</strong> a été <strong>%s</strong>.</p>
              %s
              <p style='color:#6b7280;'>Connectez-vous à ETAP StockFlow pour consulter le détail.</p>
            </body></html>
            """.formatted(couleur, statut,
                d.getNumeroDemande(), statut.toLowerCase(), motifHtml);

        try {
            PasswordUtil.envoyerEmailAvecPdf(
                    d.getEmailIntervenant(), sujet, corps, null, null);
        } catch (Exception e) {
            System.err.println("[Email] Erreur envoi intervenant : " + e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private List<Demande> execQuery(String sql, int idIntervenant) throws SQLException {
        List<Demande> list = new ArrayList<>();
        PreparedStatement ps;
        if (idIntervenant > 0) {
            ps = DatabaseConfig.getConnection().prepareStatement(sql);
            ps.setInt(1, idIntervenant);
        } else {
            ps = DatabaseConfig.getConnection().prepareStatement(sql);
        }
        try (ps; ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Demande d = mapRow(rs);
                d.setLignes(findLignes(d.getId()));
                list.add(d);
            }
        }
        return list;
    }

    private Demande mapRow(ResultSet rs) throws SQLException {
        Demande d = new Demande();
        d.setId(rs.getInt("id"));
        d.setNumeroDemande(rs.getString("numero_demande"));
        d.setIdIntervenant(rs.getInt("id_intervenant"));
        try {
            d.setNomIntervenant(rs.getString("prenom_inter")
                    + " " + rs.getString("nom_inter"));
        } catch (Exception ignored) {}
        try { d.setEmailIntervenant(rs.getString("email_inter")); }
        catch (Exception ignored) {}
        try { d.setSiteIntervenant(rs.getString("site_inter")); }
        catch (Exception ignored) {}
        try { d.setStatut(Demande.Statut.valueOf(rs.getString("statut"))); }
        catch (Exception ignored) { d.setStatut(Demande.Statut.en_attente); }
        try { d.setPriorite(Demande.Priorite.valueOf(rs.getString("priorite"))); }
        catch (Exception ignored) { d.setPriorite(Demande.Priorite.normale); }
        d.setJustification(rs.getString("justification"));
        d.setMotifRejet(rs.getString("motif_rejet"));
        Timestamp dc = rs.getTimestamp("date_creation");
        if (dc != null) d.setDateCreation(dc.toLocalDateTime());
        Timestamp dt = rs.getTimestamp("date_traitement");
        if (dt != null) d.setDateTraitement(dt.toLocalDateTime());
        d.setIdAdminTraitant(rs.getInt("id_admin_traitant"));
        return d;
    }
}
// NOTE: Ajouter cette méthode dans PasswordUtil.java
// public static void envoyerEmailAvecPdf(String dest, String sujet, String corps,
//     byte[] pdfBytes, String nomFichier) throws Exception { ... }
