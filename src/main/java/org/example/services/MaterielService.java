package org.example.services;

import org.example.config.DatabaseConfig;
import org.example.models.Materiel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaterielService {

    // ── Lecture ────────────────────────────────────────────────────────────────

    public List<Materiel> findAll() throws SQLException {
        String sql = """
            SELECT m.*, f.nom AS nom_fournisseur
            FROM materiels m
            LEFT JOIN fournisseurs f ON m.id_fournisseur = f.id
            ORDER BY m.nom ASC
            """;
        return execQuery(sql);
    }

    public List<Materiel> rechercher(String terme, String categorie, String etat)
            throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT m.*, f.nom AS nom_fournisseur
            FROM materiels m
            LEFT JOIN fournisseurs f ON m.id_fournisseur = f.id
            WHERE (m.nom LIKE ? OR m.reference LIKE ? OR m.marque LIKE ?
                   OR m.modele LIKE ? OR m.numero_serie LIKE ?)
            """);
        if (categorie != null && !categorie.isBlank()) sql.append(" AND m.categorie = ?");
        if (etat      != null && !etat.isBlank())      sql.append(" AND m.etat = ?");
        sql.append(" ORDER BY m.nom ASC");

        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql.toString())) {
            String like = "%" + terme + "%";
            ps.setString(1, like); ps.setString(2, like);
            ps.setString(3, like); ps.setString(4, like);
            ps.setString(5, like);
            int idx = 6;
            if (categorie != null && !categorie.isBlank()) ps.setString(idx++, categorie);
            if (etat      != null && !etat.isBlank())      ps.setString(idx,   etat);
            return mapResultSet(ps.executeQuery());
        }
    }

    public List<String> findCategories() throws SQLException {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT categorie FROM materiels WHERE categorie IS NOT NULL ORDER BY categorie";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) cats.add(rs.getString("categorie"));
        }
        return cats;
    }

    public List<String> findNomsFournisseurs() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id, nom FROM fournisseurs ORDER BY nom";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("nom"));
        }
        return list;
    }

    public int getIdFournisseurByNom(String nom) throws SQLException {
        String sql = "SELECT id FROM fournisseurs WHERE nom = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, nom);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : 0;
        }
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    public int countTotal()     throws SQLException { return countWhere(null); }
    public int countEnService() throws SQLException { return countWhere("etat='en_service'"); }
    public int countEnPanne()   throws SQLException { return countWhere("etat='en_panne'"); }
    public int countAlertes()   throws SQLException {
        String sql = "SELECT COUNT(*) FROM materiels WHERE quantite_stock <= seuil_alerte";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int countWhere(String where) throws SQLException {
        String sql = "SELECT COUNT(*) FROM materiels" + (where != null ? " WHERE " + where : "");
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── CRUD ───────────────────────────────────────────────────────────────────

    public void creer(Materiel m) throws SQLException {
        String sql = """
            INSERT INTO materiels
              (nom, reference, categorie, marque, modele, numero_serie,
               etat, quantite_stock, seuil_alerte, date_acquisition, id_fournisseur)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, m);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) m.setId(keys.getInt(1));
        }
    }

    public void modifier(Materiel m) throws SQLException {
        String sql = """
            UPDATE materiels
            SET nom=?, reference=?, categorie=?, marque=?, modele=?,
                numero_serie=?, etat=?, quantite_stock=?, seuil_alerte=?,
                date_acquisition=?, id_fournisseur=?
            WHERE id=?
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            setParams(ps, m);
            ps.setInt(12, m.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM materiels WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void setParams(PreparedStatement ps, Materiel m) throws SQLException {
        ps.setString(1, m.getNom());
        ps.setString(2, m.getReference());
        ps.setString(3, m.getCategorie());
        ps.setString(4, m.getMarque());
        ps.setString(5, m.getModele());
        ps.setString(6, m.getNumeroSerie());
        ps.setString(7, m.getEtat() != null ? m.getEtat().name() : Materiel.Etat.neuf.name());
        ps.setInt(8, m.getQuantiteStock());
        ps.setInt(9, m.getSeuilAlerte());
        if (m.getDateAcquisition() != null)
            ps.setDate(10, Date.valueOf(m.getDateAcquisition()));
        else
            ps.setNull(10, Types.DATE);
        if (m.getIdFournisseur() > 0) ps.setInt(11, m.getIdFournisseur());
        else                          ps.setNull(11, Types.INTEGER);
    }

    private List<Materiel> execQuery(String sql) throws SQLException {
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return mapResultSet(rs);
        }
    }

    private List<Materiel> mapResultSet(ResultSet rs) throws SQLException {
        List<Materiel> liste = new ArrayList<>();
        while (rs.next()) liste.add(mapRow(rs));
        return liste;
    }

    private Materiel mapRow(ResultSet rs) throws SQLException {
        Materiel m = new Materiel();
        m.setId(rs.getInt("id"));
        m.setNom(rs.getString("nom"));
        m.setReference(rs.getString("reference"));
        m.setCategorie(rs.getString("categorie"));
        m.setMarque(rs.getString("marque"));
        m.setModele(rs.getString("modele"));
        m.setNumeroSerie(rs.getString("numero_serie"));
        try { m.setEtat(Materiel.Etat.valueOf(rs.getString("etat"))); }
        catch (Exception ignored) { m.setEtat(Materiel.Etat.neuf); }
        m.setQuantiteStock(rs.getInt("quantite_stock"));
        m.setSeuilAlerte(rs.getInt("seuil_alerte"));
        Date da = rs.getDate("date_acquisition");
        if (da != null) m.setDateAcquisition(da.toLocalDate());
        m.setIdFournisseur(rs.getInt("id_fournisseur"));
        try { m.setNomFournisseur(rs.getString("nom_fournisseur")); }
        catch (Exception ignored) {}
        return m;
    }
}
