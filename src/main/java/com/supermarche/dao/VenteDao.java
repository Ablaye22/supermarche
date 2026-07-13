package com.supermarche.dao;

import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.LigneVente;
import com.supermarche.model.ModePaiement;
import com.supermarche.model.Paiement;
import com.supermarche.model.StatutVente;
import com.supermarche.model.Vente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VenteDao {

    public Vente creerVenteEtLignes(Connection cnx, Vente vente) throws SQLException {
        String sqlVente = """
                INSERT INTO ventes (numero_ticket, id_session, id_client, total_ht, total_tva, total_ttc, statut)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sqlVente, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, vente.getNumeroTicket());
            ps.setLong(2, vente.getIdSession());
            if (vente.getIdClient() != null) {
                ps.setInt(3, vente.getIdClient());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setBigDecimal(4, vente.getTotalHt());
            ps.setBigDecimal(5, vente.getTotalTva());
            ps.setBigDecimal(6, vente.getTotalTtc());
            ps.setString(7, vente.getStatut().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    vente.setId(rs.getLong(1));
                }
            }
        }

        String sqlLigne = """
                INSERT INTO lignes_vente (id_vente, id_produit, quantite, prix_unitaire_ttc,
                       taux_tva, remise_pourcentage, total_ligne_ttc)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sqlLigne)) {
            for (LigneVente ligne : vente.getLignes()) {
                ps.setLong(1, vente.getId());
                ps.setInt(2, ligne.getIdProduit());
                ps.setBigDecimal(3, ligne.getQuantite());
                ps.setBigDecimal(4, ligne.getPrixUnitaireTtc());
                ps.setBigDecimal(5, ligne.getTauxTva());
                ps.setBigDecimal(6, ligne.getRemisePourcentage());
                ps.setBigDecimal(7, ligne.getTotalLigneTtc());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return vente;
    }

    public void enregistrerPaiements(Connection cnx, long idVente, List<Paiement> paiements) throws SQLException {
        String sql = "INSERT INTO paiements (id_vente, mode, montant) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (Paiement paiement : paiements) {
                ps.setLong(1, idVente);
                ps.setString(2, paiement.getMode().name());
                ps.setBigDecimal(3, paiement.getMontant());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void mettreAJourStatut(Connection cnx, long idVente, StatutVente statut) throws SQLException {
        String sql = "UPDATE ventes SET statut = ? WHERE id_vente = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setLong(2, idVente);
            ps.executeUpdate();
        }
    }

    public Optional<Vente> trouverParId(Connection cnx, long id) throws SQLException {
        String sqlVente = """
                SELECT v.*, c.nom AS nom_client, c.prenom AS prenom_client
                FROM ventes v
                LEFT JOIN clients c ON c.id_client = v.id_client
                WHERE v.id_vente = ?
                """;
        Vente vente;
        try (PreparedStatement ps = cnx.prepareStatement(sqlVente)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                vente = mapperVente(rs);
            }
        }
        vente.setLignes(chargerLignes(cnx, id));
        vente.setPaiements(chargerPaiements(cnx, id));
        return Optional.of(vente);
    }

    /** Variante sans transaction explicite, pour une simple consultation (rapports, historique). */
    public Optional<Vente> trouverParId(long id) {
        try (Connection cnx = com.supermarche.config.DatabaseConfig.getConnection()) {
            return trouverParId(cnx, id);
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de la vente id=" + id, e);
        }
    }

    public List<Vente> listerParSession(long idSession) {
        String sql = """
                SELECT v.*, c.nom AS nom_client, c.prenom AS prenom_client
                FROM ventes v
                LEFT JOIN clients c ON c.id_client = v.id_client
                WHERE v.id_session = ?
                ORDER BY v.horodatage DESC
                """;
        try (Connection cnx = com.supermarche.config.DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, idSession);
            List<Vente> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultats.add(mapperVente(rs));
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des ventes de la session", e);
        }
    }

    private List<LigneVente> chargerLignes(Connection cnx, long idVente) throws SQLException {
        String sql = """
                SELECT lv.*, p.designation FROM lignes_vente lv
                JOIN produits p ON p.id_produit = lv.id_produit
                WHERE lv.id_vente = ?
                """;
        List<LigneVente> lignes = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, idVente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LigneVente l = new LigneVente();
                    l.setId(rs.getLong("id_ligne_vente"));
                    l.setIdVente(rs.getLong("id_vente"));
                    l.setIdProduit(rs.getInt("id_produit"));
                    l.setDesignationProduit(rs.getString("designation"));
                    l.setQuantite(rs.getBigDecimal("quantite"));
                    l.setPrixUnitaireTtc(rs.getBigDecimal("prix_unitaire_ttc"));
                    l.setTauxTva(rs.getBigDecimal("taux_tva"));
                    l.setRemisePourcentage(rs.getBigDecimal("remise_pourcentage"));
                    l.setTotalLigneTtc(rs.getBigDecimal("total_ligne_ttc"));
                    lignes.add(l);
                }
            }
        }
        return lignes;
    }

    private List<Paiement> chargerPaiements(Connection cnx, long idVente) throws SQLException {
        String sql = "SELECT * FROM paiements WHERE id_vente = ?";
        List<Paiement> paiements = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, idVente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Paiement p = new Paiement();
                    p.setId(rs.getLong("id_paiement"));
                    p.setIdVente(rs.getLong("id_vente"));
                    p.setMode(ModePaiement.valueOf(rs.getString("mode")));
                    p.setMontant(rs.getBigDecimal("montant"));
                    Timestamp horodatage = rs.getTimestamp("horodatage");
                    p.setHorodatage(horodatage != null ? horodatage.toLocalDateTime() : null);
                    paiements.add(p);
                }
            }
        }
        return paiements;
    }

    private Vente mapperVente(ResultSet rs) throws SQLException {
        Vente v = new Vente();
        v.setId(rs.getLong("id_vente"));
        v.setNumeroTicket(rs.getString("numero_ticket"));
        v.setIdSession(rs.getLong("id_session"));
        int idClient = rs.getInt("id_client");
        if (!rs.wasNull()) {
            v.setIdClient(idClient);
            String prenom = rs.getString("prenom_client");
            String nom = rs.getString("nom_client");
            v.setNomClient(nom != null ? ((prenom != null ? prenom + " " : "") + nom) : null);
        }
        v.setTotalHt(rs.getBigDecimal("total_ht"));
        v.setTotalTva(rs.getBigDecimal("total_tva"));
        v.setTotalTtc(rs.getBigDecimal("total_ttc"));
        v.setStatut(StatutVente.valueOf(rs.getString("statut")));
        Timestamp horodatage = rs.getTimestamp("horodatage");
        v.setHorodatage(horodatage != null ? horodatage.toLocalDateTime() : null);
        return v;
    }
}
