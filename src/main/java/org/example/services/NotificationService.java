package org.example.services;

import org.example.config.DatabaseConfig;
import org.example.models.Demande;
import org.example.utils.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service centralisé pour toutes les notifications :
 *  - Notifications in-app (table notifications)
 *  - Emails (via PasswordUtil)
 *
 * Chaque action liée aux demandes passe par ce service.
 */
public class NotificationService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ════════════════════════════════════════════════════════════
    //  NOTIFICATIONS IN-APP
    // ════════════════════════════════════════════════════════════

    /** Créer une notification in-app */
    public void creerNotification(int idDestinataire, String titre,
                                  String message, String type)
            throws SQLException {
        String sql = """
            INSERT INTO notifications (id_destinataire, titre, message, type)
            VALUES (?,?,?,?)
            """;
        try (PreparedStatement ps =
                     DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idDestinataire);
            ps.setString(2, titre);
            ps.setString(3, message);
            ps.setString(4, type);
            ps.executeUpdate();
        }
    }

    /** Lire les notifications d'un utilisateur (non lues en premier) */
    public List<NotificationItem> findByUser(int idUser) throws SQLException {
        List<NotificationItem> list = new ArrayList<>();
        String sql = """
            SELECT id, titre, message, type, lue, date_envoi
            FROM notifications
            WHERE id_destinataire = ?
            ORDER BY lue ASC, date_envoi DESC
            LIMIT 50
            """;
        try (PreparedStatement ps =
                     DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                NotificationItem n = new NotificationItem();
                n.id        = rs.getInt("id");
                n.titre     = rs.getString("titre");
                n.message   = rs.getString("message");
                n.type      = rs.getString("type");
                n.lue       = rs.getBoolean("lue");
                Timestamp d = rs.getTimestamp("date_envoi");
                n.dateEnvoi = d != null ? d.toLocalDateTime() : LocalDateTime.now();
                list.add(n);
            }
        }
        return list;
    }

    /** Nombre de notifications non lues */
    public int countNonLues(int idUser) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM notifications
            WHERE id_destinataire = ? AND lue = 0
            """;
        try (PreparedStatement ps =
                     DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Marquer une notification comme lue */
    public void marquerLue(int idNotification) throws SQLException {
        String sql = "UPDATE notifications SET lue = 1 WHERE id = ?";
        try (PreparedStatement ps =
                     DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idNotification);
            ps.executeUpdate();
        }
    }

    /** Marquer toutes les notifications d'un user comme lues */
    public void marquerToutesLues(int idUser) throws SQLException {
        String sql = "UPDATE notifications SET lue = 1 WHERE id_destinataire = ?";
        try (PreparedStatement ps =
                     DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.executeUpdate();
        }
    }

    /** Supprimer une notification */
    public void supprimer(int idNotification) throws SQLException {
        String sql = "DELETE FROM notifications WHERE id = ?";
        try (PreparedStatement ps =
                     DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, idNotification);
            ps.executeUpdate();
        }
    }

    /** Récupérer les ids de tous les admins actifs */
    public List<int[]> findAdminsActifs() throws SQLException {
        List<int[]> list = new ArrayList<>();
        String sql = """
            SELECT id, nom, prenom, email FROM utilisateurs
            WHERE role = 'admin' AND statut = 'actif'
            """;
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new int[]{ rs.getInt("id") });
            }
        }
        return list;
    }

    public List<String[]> findAdminsActifsAvecEmail() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = """
            SELECT id, nom, prenom, email FROM utilisateurs
            WHERE role = 'admin' AND statut = 'actif'
            """;
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("prenom") + " " + rs.getString("nom"),
                        rs.getString("email")
                });
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    //  ACTIONS DEMANDES — CÔTÉ INTERVENANT
    // ════════════════════════════════════════════════════════════

    /**
     * Intervenant envoie une demande.
     * → Email + notif in-app à TOUS les admins.
     */
    public void notifierNouvelleDemande(Demande d, byte[] pdfBytes)
            throws SQLException {
        List<String[]> admins = findAdminsActifsAvecEmail();
        String titre   = "Nouvelle demande de " + d.getNomIntervenant();
        String message = "La demande " + d.getNumeroDemande()
                + " de " + d.getNomIntervenant()
                + " (" + d.getSiteIntervenant() + ") est en attente de traitement.";

        String emailCorps = construireEmailDemande(d,
                "Nouvelle demande de materiel",
                "#214293",
                "Une nouvelle demande a ete soumise et attend votre traitement.",
                null);

        for (String[] admin : admins) {
            // Notif in-app
            try {
                creerNotification(Integer.parseInt(admin[0]), titre, message, "demande");
            } catch (Exception e) {
                System.err.println("[Notif] Erreur notif admin " + admin[0] + " : " + e.getMessage());
            }
            // Email avec PDF
            try {
                PasswordUtil.envoyerEmailAvecPdf(
                        admin[2],
                        "Nouvelle demande " + d.getNumeroDemande() + " — ETAP StockFlow",
                        emailCorps,
                        pdfBytes,
                        d.getNumeroDemande() + ".pdf");
            } catch (Exception e) {
                System.err.println("[Email] Erreur admin " + admin[2] + " : " + e.getMessage());
            }
        }
    }

    /**
     * Intervenant annule sa demande.
     * → Email + notif in-app à TOUS les admins.
     */
    public void notifierAnnulationDemande(Demande d) throws SQLException {
        List<String[]> admins = findAdminsActifsAvecEmail();
        String titre   = "Demande annulee par " + d.getNomIntervenant();
        String message = "La demande " + d.getNumeroDemande()
                + " de " + d.getNomIntervenant() + " a ete annulee par l'intervenant.";

        String emailCorps = construireEmailStatut(d, "Annulee", "#6b7280",
                "L'intervenant a annule sa demande.", null);

        for (String[] admin : admins) {
            try { creerNotification(Integer.parseInt(admin[0]), titre, message, "demande"); }
            catch (Exception e) { System.err.println("[Notif] " + e.getMessage()); }
            try {
                PasswordUtil.envoyerEmail(admin[2],
                        "Demande " + d.getNumeroDemande() + " annulee — ETAP StockFlow",
                        emailCorps);
            } catch (Exception e) { System.err.println("[Email] " + e.getMessage()); }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ACTIONS DEMANDES — CÔTÉ ADMIN
    // ════════════════════════════════════════════════════════════

    /**
     * Admin approuve une demande.
     * → Email + notif in-app à l'intervenant.
     */
    public void notifierDemandeApprouvee(Demande d) {
        String titre   = "Votre demande a ete approuvee";
        String message = "Votre demande " + d.getNumeroDemande()
                + " a ete approuvee. Elle sera traitee prochainement.";
        String emailCorps = construireEmailStatut(d, "Approuvee", "#16a34a",
                "Bonne nouvelle ! Votre demande a ete approuvee par l'administrateur.",
                null);

        envoyerNotifEtEmail(d.getIdIntervenant(), d.getEmailIntervenant(),
                titre, message,
                "Demande " + d.getNumeroDemande() + " approuvee — ETAP StockFlow",
                emailCorps);
    }

    /**
     * Admin rejette une demande.
     * → Email + notif in-app à l'intervenant.
     */
    public void notifierDemandeRejetee(Demande d, String motif) {
        String titre   = "Votre demande a ete rejetee";
        String message = "Votre demande " + d.getNumeroDemande()
                + " a ete rejetee."
                + (motif != null && !motif.isBlank() ? " Motif : " + motif : "");
        String emailCorps = construireEmailStatut(d, "Rejetee", "#dc2626",
                "Votre demande a ete rejetee par l'administrateur.", motif);

        envoyerNotifEtEmail(d.getIdIntervenant(), d.getEmailIntervenant(),
                titre, message,
                "Demande " + d.getNumeroDemande() + " rejetee — ETAP StockFlow",
                emailCorps);
    }

    /**
     * Admin marque la demande comme livrée.
     * → Email + notif in-app à l'intervenant.
     */
    public void notifierDemandeLivree(Demande d) {
        String titre   = "Votre demande a ete livree";
        String message = "Votre demande " + d.getNumeroDemande()
                + " a ete marquee comme livree. Vous pouvez recuperer votre materiel.";
        String emailCorps = construireEmailStatut(d, "Livree", "#0154a6",
                "Votre demande a ete livree. Vous pouvez recuperer votre materiel.",
                null);

        envoyerNotifEtEmail(d.getIdIntervenant(), d.getEmailIntervenant(),
                titre, message,
                "Demande " + d.getNumeroDemande() + " livree — ETAP StockFlow",
                emailCorps);
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ════════════════════════════════════════════════════════════

    private void envoyerNotifEtEmail(int idDestinataire, String emailDest,
                                     String titreNotif, String messageNotif,
                                     String sujetEmail, String corpsEmail) {
        // Notif in-app
        try { creerNotification(idDestinataire, titreNotif, messageNotif, "demande"); }
        catch (Exception e) { System.err.println("[Notif] " + e.getMessage()); }

        // Email
        try { PasswordUtil.envoyerEmail(emailDest, sujetEmail, corpsEmail); }
        catch (Exception e) { System.err.println("[Email] " + e.getMessage()); }
    }

    private String construireEmailDemande(Demande d, String titreEmail,
                                          String couleur, String intro,
                                          String motif) {
        String articles = "";
        if (d.getLignes() != null) {
            for (var l : d.getLignes())
                articles += "<li>" + l.getQuantite() + "x "
                        + l.getNomMateriel() + "</li>";
        }
        String motifHtml = (motif != null && !motif.isBlank())
                ? "<tr><td style='padding:6px;background:#f3f4f6;font-weight:bold;'>"
                  + "Motif</td><td style='padding:6px;'>" + motif + "</td></tr>" : "";

        return """
            <html><body style='font-family:Arial,sans-serif;color:#1a1a2e;'>
              <div style='max-width:600px;margin:0 auto;'>
                <div style='background:%s;padding:20px 30px;border-radius:8px 8px 0 0;'>
                  <h2 style='color:white;margin:0;'>ETAP StockFlow</h2>
                  <p style='color:rgba(255,255,255,0.8);margin:4px 0 0;'>
                    Departement Reseau</p>
                </div>
                <div style='background:#f8faff;padding:24px 30px;'>
                  <h3 style='color:%s;'>%s</h3>
                  <p>%s</p>
                  <table style='border-collapse:collapse;width:100%%;margin:16px 0;'>
                    <tr>
                      <td style='padding:6px;background:#f3f4f6;font-weight:bold;width:35%%;'>
                        N° Demande</td>
                      <td style='padding:6px;font-family:monospace;color:#0154a6;'>
                        %s</td>
                    </tr>
                    <tr>
                      <td style='padding:6px;background:#f3f4f6;font-weight:bold;'>
                        Demandeur</td>
                      <td style='padding:6px;'>%s — %s</td>
                    </tr>
                    <tr>
                      <td style='padding:6px;background:#f3f4f6;font-weight:bold;'>
                        Date</td>
                      <td style='padding:6px;'>%s</td>
                    </tr>
                    <tr>
                      <td style='padding:6px;background:#f3f4f6;font-weight:bold;'>
                        Priorite</td>
                      <td style='padding:6px;'>%s</td>
                    </tr>
                    <tr>
                      <td style='padding:6px;background:#f3f4f6;font-weight:bold;'>
                        Justification</td>
                      <td style='padding:6px;'>%s</td>
                    </tr>
                    %s
                  </table>
                  <h4 style='color:#214293;'>Articles demandes :</h4>
                  <ul style='line-height:1.8;'>%s</ul>
                </div>
                <div style='background:#e8f0fb;padding:12px 30px;border-radius:0 0 8px 8px;
                     text-align:center;'>
                  <p style='color:#6b7280;font-size:12px;margin:0;'>
                    ETAP — Entreprise Tunisienne d'Activites Petrolieres</p>
                </div>
              </div>
            </body></html>
            """.formatted(
                couleur, couleur, titreEmail, intro,
                d.getNumeroDemande(),
                d.getNomIntervenant() != null ? d.getNomIntervenant() : "—",
                d.getSiteIntervenant() != null ? d.getSiteIntervenant() : "—",
                d.getDateCreation() != null ? d.getDateCreation().format(FMT) : "—",
                d.getPrioriteLabel(),
                d.getJustification() != null ? d.getJustification() : "—",
                motifHtml, articles
        );
    }

    private String construireEmailStatut(Demande d, String statut,
                                         String couleur, String message,
                                         String motif) {
        return construireEmailDemande(d,
                "Demande " + statut, couleur, message, motif);
    }

    // ════════════════════════════════════════════════════════════
    //  MODÈLE NOTIFICATION ITEM (pour l'affichage)
    // ════════════════════════════════════════════════════════════

    public static class NotificationItem {
        public int           id;
        public String        titre;
        public String        message;
        public String        type;
        public boolean       lue;
        public LocalDateTime dateEnvoi;

        public String getTempsRelatif() {
            if (dateEnvoi == null) return "";
            long minutes = java.time.Duration.between(dateEnvoi,
                    LocalDateTime.now()).toMinutes();
            if (minutes < 1)  return "A l'instant";
            if (minutes < 60) return "Il y a " + minutes + " min";
            long heures = minutes / 60;
            if (heures < 24)  return "Il y a " + heures + "h";
            long jours = heures / 24;
            if (jours < 7)    return "Il y a " + jours + " j";
            return dateEnvoi.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }
}
