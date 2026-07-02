package org.example.services;

import org.example.config.DatabaseConfig;
import org.example.models.Fournisseur;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FournisseurService {

    // ── CRUD ───────────────────────────────────────────────────────────────────

    public List<Fournisseur> findAll() throws SQLException {
        List<Fournisseur> liste = new ArrayList<>();
        String sql = "SELECT * FROM fournisseurs ORDER BY nom ASC";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapRow(rs));
        }
        return liste;
    }

    public List<Fournisseur> rechercher(String terme) throws SQLException {
        List<Fournisseur> liste = new ArrayList<>();
        String sql = """
            SELECT * FROM fournisseurs
            WHERE nom       LIKE ?
               OR email     LIKE ?
               OR telephone LIKE ?
               OR adresse   LIKE ?
               OR specialite LIKE ?
            ORDER BY nom ASC
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            String like = "%" + terme + "%";
            for (int i = 1; i <= 5; i++) ps.setString(i, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapRow(rs));
        }
        return liste;
    }

    public void creer(Fournisseur f) throws SQLException {
        String sql = """
            INSERT INTO fournisseurs (nom, email, telephone, adresse, specialite)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getNom());
            ps.setString(2, f.getEmail());
            ps.setString(3, f.getTelephone());
            ps.setString(4, f.getAdresse());
            ps.setString(5, f.getSpecialite());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) f.setId(keys.getInt(1));
        }
    }

    public void modifier(Fournisseur f) throws SQLException {
        String sql = """
            UPDATE fournisseurs
            SET nom=?, email=?, telephone=?, adresse=?, specialite=?
            WHERE id=?
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, f.getNom());
            ps.setString(2, f.getEmail());
            ps.setString(3, f.getTelephone());
            ps.setString(4, f.getAdresse());
            ps.setString(5, f.getSpecialite());
            ps.setInt(6, f.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM fournisseurs WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM fournisseurs";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Mapper ─────────────────────────────────────────────────────────────────

    private Fournisseur mapRow(ResultSet rs) throws SQLException {
        Fournisseur f = new Fournisseur();
        f.setId(rs.getInt("id"));
        f.setNom(rs.getString("nom"));
        f.setEmail(rs.getString("email"));
        f.setTelephone(rs.getString("telephone"));
        f.setAdresse(rs.getString("adresse"));
        f.setSpecialite(rs.getString("specialite"));
        Timestamp da = rs.getTimestamp("date_ajout");
        if (da != null) f.setDateAjout(da.toLocalDateTime());
        return f;
    }
}
