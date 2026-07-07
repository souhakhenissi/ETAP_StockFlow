package org.example.services;

import org.example.config.DatabaseConfig;
import org.example.models.MouvementStock;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StockService {

    // ── Génération numéro bon ──────────────────────────────────────────────────
    public String genererNumeroBon(MouvementStock.Type type) {
        String prefix = type == MouvementStock.Type.entree ? "ENT" : "SOR";
        String date   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid   = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + date + "-" + uuid;
    }

    // ── Lire les entrées ───────────────────────────────────────────────────────
    public List<MouvementStock> findEntrees() throws SQLException {
        String sql = """
            SELECT ms.*,
                   m.nom       AS nom_materiel,
                   m.reference AS ref_materiel,
                   f.nom       AS nom_fournisseur
            FROM   mouvements_stock ms
            LEFT JOIN materiels    m ON ms.id_materiel    = m.id
            LEFT JOIN fournisseurs f ON ms.id_fournisseur = f.id
            WHERE  ms.type = 'entree'
            ORDER  BY ms.date_creation DESC
            """;
        return execQuery(sql);
    }

    // ── Lire les sorties ───────────────────────────────────────────────────────
    public List<MouvementStock> findSorties() throws SQLException {
        String sql = """
            SELECT ms.*,
                   m.nom       AS nom_materiel,
                   m.reference AS ref_materiel,
                   NULL        AS nom_fournisseur
            FROM   mouvements_stock ms
            LEFT JOIN materiels m ON ms.id_materiel = m.id
            WHERE  ms.type = 'sortie'
            ORDER  BY ms.date_creation DESC
            """;
        return execQuery(sql);
    }

    // ── Demandes en attente (non encore traitées par un bon de sortie) ─────────
    public List<String> findNumerosDemandes() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = """
            SELECT numero_demande
            FROM   demandes_en_attente
            WHERE  numero_demande NOT IN (
                       SELECT COALESCE(numero_demande, '')
                       FROM   mouvements_stock
                       WHERE  type = 'sortie'
                         AND  numero_demande IS NOT NULL
                   )
            ORDER  BY date_creation DESC
            """;
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("numero_demande"));
        } catch (SQLException e) {
            // La table peut ne pas encore exister — on retourne une liste vide
            System.err.println("[StockService] demandes_en_attente introuvable : " + e.getMessage());
        }
        return list;
    }

    // ── Matériels (id, nom, référence) ────────────────────────────────────────
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

    // ── Fournisseurs (id, nom) ─────────────────────────────────────────────────
    public List<String[]> findFournisseurs() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT id, nom FROM fournisseurs ORDER BY nom";
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("nom")
                });
        }
        return list;
    }

    // ── Alertes stock bas ──────────────────────────────────────────────────────
    public List<String[]> findAlertesStockBas() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = """
            SELECT nom, reference, quantite_stock, seuil_alerte
            FROM   materiels
            WHERE  quantite_stock <= seuil_alerte
            ORDER  BY (quantite_stock - seuil_alerte) ASC
            LIMIT  10
            """;
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(new String[]{
                        rs.getString("nom"),
                        rs.getString("reference"),
                        String.valueOf(rs.getInt("quantite_stock")),
                        String.valueOf(rs.getInt("seuil_alerte"))
                });
        }
        return list;
    }

    // ── Créer un mouvement ─────────────────────────────────────────────────────
    public void creer(MouvementStock m) throws SQLException {
        String sql = """
            INSERT INTO mouvements_stock
              (numero_bon, type, id_materiel, quantite, prix_unitaire,
               id_fournisseur, numero_demande, site_etap, observation)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getNumeroBon());
            ps.setString(2, m.getType().name());
            ps.setInt(3, m.getIdMateriel());
            ps.setInt(4, m.getQuantite());
            ps.setDouble(5, m.getPrixUnitaire());
            setNullableInt(ps, 6, m.getIdFournisseur());
            ps.setString(7, m.getNumeroDemande());
            ps.setString(8, m.getSiteEtap());
            ps.setString(9, m.getObservation());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) m.setId(keys.getInt(1));
        }
        // Mettre à jour le stock du matériel
        ajusterStock(m.getIdMateriel(), m.getQuantite(), m.getType());
    }

    // ── Modifier un mouvement ──────────────────────────────────────────────────
    // ancienIdMateriel et ancienneQte sont nécessaires pour annuler l'effet précédent
    public void modifier(MouvementStock m, int ancienIdMateriel,
                         int ancienneQte, MouvementStock.Type ancienType) throws SQLException {
        // 1. Annuler l'ancien effet
        MouvementStock.Type inverse = ancienType == MouvementStock.Type.entree
                ? MouvementStock.Type.sortie : MouvementStock.Type.entree;
        ajusterStock(ancienIdMateriel, ancienneQte, inverse);

        // 2. Mettre à jour la ligne
        String sql = """
            UPDATE mouvements_stock
            SET    id_materiel=?, quantite=?, prix_unitaire=?,
                   id_fournisseur=?, numero_demande=?, site_etap=?, observation=?
            WHERE  id=?
            """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, m.getIdMateriel());
            ps.setInt(2, m.getQuantite());
            ps.setDouble(3, m.getPrixUnitaire());
            setNullableInt(ps, 4, m.getIdFournisseur());
            ps.setString(5, m.getNumeroDemande());
            ps.setString(6, m.getSiteEtap());
            ps.setString(7, m.getObservation());
            ps.setInt(8, m.getId());
            ps.executeUpdate();
        }

        // 3. Appliquer le nouvel effet
        ajusterStock(m.getIdMateriel(), m.getQuantite(), m.getType());
    }

    // ── Supprimer un mouvement ─────────────────────────────────────────────────
    public void supprimer(MouvementStock m) throws SQLException {
        // Annuler l'effet sur le stock
        MouvementStock.Type inverse = m.getType() == MouvementStock.Type.entree
                ? MouvementStock.Type.sortie : MouvementStock.Type.entree;
        ajusterStock(m.getIdMateriel(), m.getQuantite(), inverse);

        String sql = "DELETE FROM mouvements_stock WHERE id=?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, m.getId());
            ps.executeUpdate();
        }
    }

    // ── Ajuster quantité stock ─────────────────────────────────────────────────
    private void ajusterStock(int idMateriel, int qte, MouvementStock.Type type)
            throws SQLException {
        String op  = type == MouvementStock.Type.entree ? "+" : "-";
        String sql = "UPDATE materiels SET quantite_stock = quantite_stock "
                + op + " ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, qte);
            ps.setInt(2, idMateriel);
            ps.executeUpdate();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void setNullableInt(PreparedStatement ps, int idx, int val) throws SQLException {
        if (val > 0) ps.setInt(idx, val);
        else         ps.setNull(idx, Types.INTEGER);
    }

    private List<MouvementStock> execQuery(String sql) throws SQLException {
        List<MouvementStock> list = new ArrayList<>();
        try (Statement st = DatabaseConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private MouvementStock mapRow(ResultSet rs) throws SQLException {
        MouvementStock m = new MouvementStock();
        m.setId(rs.getInt("id"));
        m.setNumeroBon(rs.getString("numero_bon"));
        try { m.setType(MouvementStock.Type.valueOf(rs.getString("type"))); }
        catch (Exception ignored) {}
        m.setIdMateriel(rs.getInt("id_materiel"));
        m.setNomMateriel(rs.getString("nom_materiel"));
        m.setReferenceMateriel(rs.getString("ref_materiel"));
        m.setQuantite(rs.getInt("quantite"));
        m.setPrixUnitaire(rs.getDouble("prix_unitaire"));
        m.setIdFournisseur(rs.getInt("id_fournisseur"));
        try { m.setNomFournisseur(rs.getString("nom_fournisseur")); }
        catch (Exception ignored) {}
        m.setNumeroDemande(rs.getString("numero_demande"));
        m.setSiteEtap(rs.getString("site_etap"));
        m.setObservation(rs.getString("observation"));
        Timestamp dc = rs.getTimestamp("date_creation");
        if (dc != null) m.setDateCreation(dc.toLocalDateTime());
        return m;
    }
}
