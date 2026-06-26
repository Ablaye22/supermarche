package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.Categorie;
import com.supermarche.model.Fournisseur;
import com.supermarche.model.Produit;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProduitDao {

    public Produit creer(Produit p) {
        String sql = """
                INSERT INTO produits (code_barre, designation, description, id_categorie,
                       id_fournisseur_principal, prix_achat_ht, prix_vente_ttc, taux_tva,
                       unite, seuil_alerte_stock, actif)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplirParametres(ps, p);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
            return p;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la creation du produit", e);
        }
    }

    public void modifier(Produit p) {
        String sql = """
                UPDATE produits SET code_barre = ?, designation = ?, description = ?,
                       id_categorie = ?, id_fournisseur_principal = ?, prix_achat_ht = ?,
                       prix_vente_ttc = ?, taux_tva = ?, unite = ?, seuil_alerte_stock = ?, actif = ?
                WHERE id_produit = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            remplirParametres(ps, p);
            ps.setInt(12, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la modification du produit id=" + p.getId(), e);
        }
    }

    private void remplirParametres(PreparedStatement ps, Produit p) throws SQLException {
        ps.setString(1, p.getCodeBarre());
        ps.setString(2, p.getDesignation());
        ps.setString(3, p.getDescription());
        if (p.getCategorie() != null && p.getCategorie().getId() != null) {
            ps.setInt(4, p.getCategorie().getId());
        } else {
            ps.setNull(4, java.sql.Types.INTEGER);
        }
        if (p.getFournisseurPrincipal() != null && p.getFournisseurPrincipal().getId() != null) {
            ps.setInt(5, p.getFournisseurPrincipal().getId());
        } else {
            ps.setNull(5, java.sql.Types.INTEGER);
        }
        ps.setBigDecimal(6, p.getPrixAchatHt());
        ps.setBigDecimal(7, p.getPrixVenteTtc());
        ps.setBigDecimal(8, p.getTauxTva());
        ps.setString(9, p.getUnite());
        ps.setInt(10, p.getSeuilAlerteStock());
        ps.setBoolean(11, p.isActif());
    }

    public Optional<Produit> trouverParId(int id) {
        String sql = baseSelect() + " WHERE p.id_produit = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche du produit id=" + id, e);
        }
    }

    public Optional<Produit> trouverParCodeBarre(String codeBarre) {
        String sql = baseSelect() + " WHERE p.code_barre = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, codeBarre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche du produit code-barre=" + codeBarre, e);
        }
    }

    public List<Produit> rechercherParDesignation(String motCle, boolean inclureInactifs) {
        StringBuilder sql = new StringBuilder(baseSelect());
        sql.append(" WHERE p.designation LIKE ?");
        if (!inclureInactifs) {
            sql.append(" AND p.actif = TRUE");
        }
        sql.append(" ORDER BY p.designation LIMIT 200");

        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            ps.setString(1, "%" + motCle + "%");
            List<Produit> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultats.add(mapper(rs));
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de produits", e);
        }
    }

    public List<Produit> listerTous(boolean inclureInactifs) {
        String sql = baseSelect() + (inclureInactifs ? "" : " WHERE p.actif = TRUE") + " ORDER BY p.designation";
        List<Produit> resultats = new ArrayList<>();
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultats.add(mapper(rs));
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des produits", e);
        }
    }

    /** Produits dont le stock dans un depot donne est sous le seuil d'alerte. */
    public List<Produit> listerEnAlerteStock(int idDepot) {
        String sql = baseSelect() + """
                 JOIN stocks s ON s.id_produit = p.id_produit
                WHERE s.id_depot = ? AND p.actif = TRUE AND s.quantite <= p.seuil_alerte_stock
                ORDER BY p.designation
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idDepot);
            List<Produit> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultats.add(mapper(rs));
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche des produits en alerte de stock", e);
        }
    }

    private String baseSelect() {
        return """
                SELECT p.*, c.nom AS c_nom, f.raison_sociale AS f_raison_sociale, f.id_fournisseur AS f_id
                FROM produits p
                LEFT JOIN categories c ON c.id_categorie = p.id_categorie
                LEFT JOIN fournisseurs f ON f.id_fournisseur = p.id_fournisseur_principal
                """;
    }

    private Produit mapper(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setId(rs.getInt("id_produit"));
        p.setCodeBarre(rs.getString("code_barre"));
        p.setDesignation(rs.getString("designation"));
        p.setDescription(rs.getString("description"));

        int idCategorie = rs.getInt("id_categorie");
        if (!rs.wasNull()) {
            Categorie c = new Categorie(idCategorie, rs.getString("c_nom"));
            p.setCategorie(c);
        }

        int idFournisseur = rs.getInt("f_id");
        if (!rs.wasNull()) {
            Fournisseur f = new Fournisseur();
            f.setId(idFournisseur);
            f.setRaisonSociale(rs.getString("f_raison_sociale"));
            p.setFournisseurPrincipal(f);
        }

        p.setPrixAchatHt(rs.getBigDecimal("prix_achat_ht"));
        p.setPrixVenteTtc(rs.getBigDecimal("prix_vente_ttc"));
        p.setTauxTva(rs.getBigDecimal("taux_tva"));
        p.setUnite(rs.getString("unite"));
        p.setSeuilAlerteStock(rs.getInt("seuil_alerte_stock"));
        p.setActif(rs.getBoolean("actif"));
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        p.setDateCreation(dateCreation != null ? dateCreation.toLocalDateTime() : null);
        Timestamp dateModification = rs.getTimestamp("date_modification");
        p.setDateModification(dateModification != null ? dateModification.toLocalDateTime() : null);
        return p;
    }
}
