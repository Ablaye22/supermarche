package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.exception.StockInsuffisantException;
import com.supermarche.model.MouvementStock;
import com.supermarche.model.Stock;
import com.supermarche.model.TypeMouvement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class StockDao {

    /**
     * @throws StockInsuffisantException si la quantite demandee n'est pas disponible
     */
    public void decrementerPourVente(Connection cnx, int idProduit, int idDepot, int quantite,
                                      Integer idUtilisateur, String reference) throws SQLException {
        int quantiteActuelle = lireEtVerrouiller(cnx, idProduit, idDepot);
        if (quantiteActuelle < quantite) {
            throw new StockInsuffisantException(
                    "Stock insuffisant pour le produit id=" + idProduit
                            + " (disponible: " + quantiteActuelle + ", demande: " + quantite + ")");
        }
        int nouvelleQuantite = quantiteActuelle - quantite;
        mettreAJourQuantite(cnx, idProduit, idDepot, nouvelleQuantite);
        enregistrerMouvement(cnx, idProduit, idDepot, TypeMouvement.VENTE, quantite,
                nouvelleQuantite, reference, idUtilisateur, null);
    }

    public void reintegrerApresAnnulation(Connection cnx, int idProduit, int idDepot, int quantite,
                                           Integer idUtilisateur, String reference) throws SQLException {
        int quantiteActuelle = lireEtVerrouiller(cnx, idProduit, idDepot);
        int nouvelleQuantite = quantiteActuelle + quantite;
        mettreAJourQuantite(cnx, idProduit, idDepot, nouvelleQuantite);
        enregistrerMouvement(cnx, idProduit, idDepot, TypeMouvement.RETOUR, quantite,
                nouvelleQuantite, reference, idUtilisateur, "Annulation/remboursement de vente");
    }

    public void enregistrerEntree(int idProduit, int idDepot, int quantite, Integer idUtilisateur, String reference) {
        try (Connection cnx = DatabaseConfig.getConnection()) {
            cnx.setAutoCommit(false);
            try {
                enregistrerEntree(cnx, idProduit, idDepot, quantite, idUtilisateur, reference);
                cnx.commit();
            } catch (Exception e) {
                cnx.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de l'enregistrement de l'entree de stock", e);
        }
    }

    public void enregistrerEntree(Connection cnx, int idProduit, int idDepot, int quantite,
                                   Integer idUtilisateur, String reference) throws SQLException {
        int quantiteActuelle = lireEtVerrouiller(cnx, idProduit, idDepot);
        int nouvelleQuantite = quantiteActuelle + quantite;
        mettreAJourQuantite(cnx, idProduit, idDepot, nouvelleQuantite);
        enregistrerMouvement(cnx, idProduit, idDepot, TypeMouvement.ENTREE, quantite,
                nouvelleQuantite, reference, idUtilisateur, null);
    }

    public void ajusterStock(int idProduit, int idDepot, int nouvelleQuantite, Integer idUtilisateur, String commentaire) {
        try (Connection cnx = DatabaseConfig.getConnection()) {
            cnx.setAutoCommit(false);
            try {
                int quantiteActuelle = lireEtVerrouiller(cnx, idProduit, idDepot);
                int difference = nouvelleQuantite - quantiteActuelle;
                mettreAJourQuantite(cnx, idProduit, idDepot, nouvelleQuantite);
                enregistrerMouvement(cnx, idProduit, idDepot, TypeMouvement.AJUSTEMENT, Math.abs(difference),
                        nouvelleQuantite, null, idUtilisateur, commentaire);
                cnx.commit();
            } catch (Exception e) {
                cnx.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de l'ajustement de stock", e);
        }
    }

    private int lireEtVerrouiller(Connection cnx, int idProduit, int idDepot) throws SQLException {
        String sqlLire = "SELECT quantite FROM stocks WHERE id_produit = ? AND id_depot = ? FOR UPDATE";
        try (PreparedStatement ps = cnx.prepareStatement(sqlLire)) {
            ps.setInt(1, idProduit);
            ps.setInt(2, idDepot);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantite");
                }
            }
        }
        String sqlCreer = "INSERT INTO stocks (id_produit, id_depot, quantite) VALUES (?, ?, 0)";
        try (PreparedStatement ps = cnx.prepareStatement(sqlCreer)) {
            ps.setInt(1, idProduit);
            ps.setInt(2, idDepot);
            ps.executeUpdate();
        }
        return 0;
    }

    private void mettreAJourQuantite(Connection cnx, int idProduit, int idDepot, int nouvelleQuantite) throws SQLException {
        String sql = "UPDATE stocks SET quantite = ? WHERE id_produit = ? AND id_depot = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, nouvelleQuantite);
            ps.setInt(2, idProduit);
            ps.setInt(3, idDepot);
            ps.executeUpdate();
        }
    }

    private void enregistrerMouvement(Connection cnx, int idProduit, int idDepot, TypeMouvement type,
                                       int quantite, int quantiteApres, String reference,
                                       Integer idUtilisateur, String commentaire) throws SQLException {
        String sql = """
                INSERT INTO mouvements_stock (id_produit, id_depot, type_mouvement, quantite,
                       quantite_apres, reference, id_utilisateur, commentaire)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProduit);
            ps.setInt(2, idDepot);
            ps.setString(3, type.name());
            ps.setInt(4, quantite);
            ps.setInt(5, quantiteApres);
            ps.setString(6, reference);
            if (idUtilisateur != null) {
                ps.setInt(7, idUtilisateur);
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.setString(8, commentaire);
            ps.executeUpdate();
        }
    }

    public int lireQuantite(int idProduit, int idDepot) {
        String sql = "SELECT quantite FROM stocks WHERE id_produit = ? AND id_depot = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProduit);
            ps.setInt(2, idDepot);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("quantite") : 0;
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la lecture du stock", e);
        }
    }

    public List<Stock> listerParDepot(int idDepot) {
        String sql = """
                SELECT s.*, p.designation FROM stocks s
                JOIN produits p ON p.id_produit = s.id_produit
                WHERE s.id_depot = ?
                ORDER BY p.designation
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idDepot);
            List<Stock> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Stock s = new Stock();
                    s.setIdProduit(rs.getInt("id_produit"));
                    s.setIdDepot(rs.getInt("id_depot"));
                    s.setQuantite(rs.getInt("quantite"));
                    s.setDesignationProduit(rs.getString("designation"));
                    resultats.add(s);
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des stocks", e);
        }
    }

    public List<MouvementStock> historiqueParProduit(int idProduit, int limite) {
        String sql = """
                SELECT m.*, p.designation FROM mouvements_stock m
                JOIN produits p ON p.id_produit = m.id_produit
                WHERE m.id_produit = ?
                ORDER BY m.horodatage DESC
                LIMIT ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProduit);
            ps.setInt(2, limite);
            List<MouvementStock> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultats.add(mapperMouvement(rs));
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la lecture de l'historique des mouvements", e);
        }
    }

    private MouvementStock mapperMouvement(ResultSet rs) throws SQLException {
        MouvementStock m = new MouvementStock();
        m.setId(rs.getLong("id_mouvement"));
        m.setIdProduit(rs.getInt("id_produit"));
        m.setDesignationProduit(rs.getString("designation"));
        m.setIdDepot(rs.getInt("id_depot"));
        m.setTypeMouvement(TypeMouvement.valueOf(rs.getString("type_mouvement")));
        m.setQuantite(rs.getInt("quantite"));
        m.setQuantiteApres(rs.getInt("quantite_apres"));
        m.setReference(rs.getString("reference"));
        int idUtilisateur = rs.getInt("id_utilisateur");
        m.setIdUtilisateur(rs.wasNull() ? null : idUtilisateur);
        m.setCommentaire(rs.getString("commentaire"));
        Timestamp horodatage = rs.getTimestamp("horodatage");
        m.setHorodatage(horodatage != null ? horodatage.toLocalDateTime() : null);
        return m;
    }
}
