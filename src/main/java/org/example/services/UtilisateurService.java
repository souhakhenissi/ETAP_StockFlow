package org.example.services;

import org.example.config.DatabaseConfig;
import org.example.models.Utilisateur;
import org.example.utils.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService {

    // ── Authentification ───────────────────────────────────────────────────────

    /**
     * Vérifie email + mot de passe. Retourne l'utilisateur ou null.
     */
    public Utilisateur authentifier(String email, String motDePasse) throws SQLException {
        String sql = "SELECT * FROM utilisateurs WHERE email = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("mot_de_passe");
                if (PasswordUtil.verify(motDePasse, hash)) {
                    Utilisateur u = mapRow(rs);
                    if (u.getStatut() == Utilisateur.Statut.bloque) {
                        throw new SQLException("COMPTE_BLOQUE");
                    }
                    if (u.getStatut() == Utilisateur.Statut.inactif) {
                        throw new SQLException("COMPTE_INACTIF");
                    }
                    return u;
                }
            }
        }
        return null;
    }

    // ── Mot de passe oublié ────────────────────────────────────────────────────

    public boolean emailExiste(String email) throws SQLException {
        String sql = "SELECT id FROM utilisateurs WHERE email = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            return ps.executeQuery().next();
        }
    }

    public void enregistrerResetToken(String email, String token) throws SQLException {
        String sql = "UPDATE utilisateurs SET reset_token=?, reset_expiry=? WHERE email=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().plusMinutes(30)));
            ps.setString(3, email.trim().toLowerCase());
            ps.executeUpdate();
        }
    }

    public Utilisateur trouverParResetToken(String token) throws SQLException {
        String sql = "SELECT * FROM utilisateurs WHERE reset_token=? AND reset_expiry > NOW()";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    public void reinitialiserMotDePasse(String token, String nouveauMotDePasse) throws SQLException {
        String sql = "UPDATE utilisateurs SET mot_de_passe=?, reset_token=NULL, reset_expiry=NULL WHERE reset_token=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(nouveauMotDePasse));
            ps.setString(2, token);
            ps.executeUpdate();
        }
    }

    // ── CRUD Utilisateurs ──────────────────────────────────────────────────────

    public List<Utilisateur> findAll() throws SQLException {
        List<Utilisateur> liste = new ArrayList<>();
        String sql = "SELECT * FROM utilisateurs ORDER BY date_creation DESC";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapRow(rs));
        }
        return liste;
    }

    public List<Utilisateur> rechercher(String terme, String role, String statut) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM utilisateurs WHERE (nom LIKE ? OR prenom LIKE ? OR email LIKE ?)");
        if (role   != null && !role.isEmpty())   sql.append(" AND role=?");
        if (statut != null && !statut.isEmpty())  sql.append(" AND statut=?");
        sql.append(" ORDER BY date_creation DESC");

        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql.toString())) {
            String like = "%" + terme + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            int idx = 4;
            if (role   != null && !role.isEmpty())   ps.setString(idx++, role);
            if (statut != null && !statut.isEmpty())  ps.setString(idx,   statut);
            List<Utilisateur> liste = new ArrayList<>();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapRow(rs));
            return liste;
        }
    }

    public Utilisateur findById(int id) throws SQLException {
        String sql = "SELECT * FROM utilisateurs WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    public void creer(Utilisateur u) throws SQLException {
        String sql = """
            INSERT INTO utilisateurs
              (nom, prenom, email, mot_de_passe, role, site_affecte, statut, photo)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail().trim().toLowerCase());
            ps.setString(4, PasswordUtil.hash(u.getMotDePasse()));
            ps.setString(5, u.getRole().name());
            ps.setString(6, u.getSiteAffecte());
            ps.setString(7, u.getStatut().name());
            ps.setString(8, u.getPhoto());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) u.setId(keys.getInt(1));
        }
    }

    public void modifier(Utilisateur u) throws SQLException {
        String sql = """
            UPDATE utilisateurs
            SET nom=?, prenom=?, email=?, role=?, site_affecte=?, statut=?, photo=?
            WHERE id=?
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail().trim().toLowerCase());
            ps.setString(4, u.getRole().name());
            ps.setString(5, u.getSiteAffecte());
            ps.setString(6, u.getStatut().name());
            ps.setString(7, u.getPhoto());
            ps.setInt(8, u.getId());
            ps.executeUpdate();
        }
    }

    public void modifierMotDePasse(int id, String nouveauMdp) throws SQLException {
        String sql = "UPDATE utilisateurs SET mot_de_passe=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(nouveauMdp));
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void bloquer(int id) throws SQLException   { changerStatut(id, "bloque"); }
    public void debloquer(int id) throws SQLException { changerStatut(id, "actif"); }
    public void desactiver(int id) throws SQLException{ changerStatut(id, "inactif"); }

    private void changerStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE utilisateurs SET statut=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM utilisateurs WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── Statistiques pour le tableau de bord ──────────────────────────────────

    public int countTotal()        throws SQLException { return countWhere(null); }
    public int countActifs()       throws SQLException { return countWhere("statut='actif'"); }
    public int countBloques()      throws SQLException { return countWhere("statut='bloque'"); }
    public int countIntervenants() throws SQLException { return countWhere("role='intervenant'"); }

    private int countWhere(String where) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilisateurs" + (where != null ? " WHERE " + where : "");
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Reconnaissance faciale ─────────────────────────────────────────────────

    public void enregistrerFaceEncoding(int id, String encodingJson) throws SQLException {
        String sql = "UPDATE utilisateurs SET face_encoding=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, encodingJson);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<Utilisateur> findAvecFaceEncoding() throws SQLException {
        List<Utilisateur> liste = new ArrayList<>();
        String sql = "SELECT * FROM utilisateurs WHERE face_encoding IS NOT NULL AND statut='actif'";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapRow(rs));
        }
        return liste;
    }

    // ── Mapper ResultSet → Utilisateur ─────────────────────────────────────────

    private Utilisateur mapRow(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setRole(Utilisateur.Role.valueOf(rs.getString("role")));
        u.setSiteAffecte(rs.getString("site_affecte"));
        u.setStatut(Utilisateur.Statut.valueOf(rs.getString("statut")));
        u.setPhoto(rs.getString("photo"));
        u.setFaceEncoding(rs.getString("face_encoding"));
        u.setResetToken(rs.getString("reset_token"));
        Timestamp exp = rs.getTimestamp("reset_expiry");
        if (exp != null) u.setResetExpiry(exp.toLocalDateTime());
        Timestamp dc = rs.getTimestamp("date_creation");
        if (dc != null) u.setDateCreation(dc.toLocalDateTime());
        return u;
    }
}

