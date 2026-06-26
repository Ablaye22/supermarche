package com.supermarche.service;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.MouvementStock;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Genere des indicateurs de gestion a partir des donnees de vente et de
 * stock. Les requetes sont volontairement ecrites en SQL direct (agregations
 * cote base de donnees) plutot que de charger toutes les lignes en memoire
 * pour les agreger en Java : plus rapide et plus sobre en memoire sur de
 * gros volumes de ventes.
 */
public class RapportService {

    private final AuthService authService;

    public RapportService(AuthService authService) {
        this.authService = authService;
    }

    public record VentilationParJour(java.time.LocalDate jour, BigDecimal totalTtc, long nombreVentes) {
    }

    public record ProduitVendu(String designation, BigDecimal quantiteTotale, BigDecimal chiffreAffairesTtc) {
    }

    public record SyntheseVentes(BigDecimal chiffreAffairesTtc, BigDecimal chiffreAffairesHt,
                                  BigDecimal totalTva, long nombreVentes, BigDecimal panierMoyen) {
    }

    public SyntheseVentes syntheseVentes(LocalDateTime depuis, LocalDateTime jusqua) {
        authService.exigerPermission("RAPPORT_VOIR");
        String sql = """
                SELECT COALESCE(SUM(total_ttc), 0) AS ca_ttc, COALESCE(SUM(total_ht), 0) AS ca_ht,
                       COALESCE(SUM(total_tva), 0) AS total_tva, COUNT(*) AS nb_ventes
                FROM ventes
                WHERE statut = 'VALIDEE' AND horodatage BETWEEN ? AND ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(depuis));
            ps.setTimestamp(2, Timestamp.valueOf(jusqua));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal caTtc = rs.getBigDecimal("ca_ttc");
                long nbVentes = rs.getLong("nb_ventes");
                BigDecimal panierMoyen = nbVentes > 0
                        ? caTtc.divide(BigDecimal.valueOf(nbVentes), 2, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                return new SyntheseVentes(caTtc, rs.getBigDecimal("ca_ht"), rs.getBigDecimal("total_tva"), nbVentes, panierMoyen);
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors du calcul de la synthese des ventes", e);
        }
    }

    public List<VentilationParJour> ventesParJour(LocalDateTime depuis, LocalDateTime jusqua) {
        authService.exigerPermission("RAPPORT_VOIR");
        String sql = """
                SELECT DATE(horodatage) AS jour, SUM(total_ttc) AS total, COUNT(*) AS nb
                FROM ventes
                WHERE statut = 'VALIDEE' AND horodatage BETWEEN ? AND ?
                GROUP BY DATE(horodatage)
                ORDER BY jour
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(depuis));
            ps.setTimestamp(2, Timestamp.valueOf(jusqua));
            List<VentilationParJour> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultats.add(new VentilationParJour(
                            rs.getDate("jour").toLocalDate(),
                            rs.getBigDecimal("total"),
                            rs.getLong("nb")));
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors du calcul de la ventilation par jour", e);
        }
    }

    public List<ProduitVendu> meilleuresVentes(LocalDateTime depuis, LocalDateTime jusqua, int limite) {
        authService.exigerPermission("RAPPORT_VOIR");
        String sql = """
                SELECT p.designation, SUM(lv.quantite) AS qte_totale, SUM(lv.total_ligne_ttc) AS ca_total
                FROM lignes_vente lv
                JOIN ventes v ON v.id_vente = lv.id_vente
                JOIN produits p ON p.id_produit = lv.id_produit
                WHERE v.statut = 'VALIDEE' AND v.horodatage BETWEEN ? AND ?
                GROUP BY p.id_produit, p.designation
                ORDER BY ca_total DESC
                LIMIT ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(depuis));
            ps.setTimestamp(2, Timestamp.valueOf(jusqua));
            ps.setInt(3, limite);
            List<ProduitVendu> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultats.add(new ProduitVendu(
                            rs.getString("designation"),
                            rs.getBigDecimal("qte_totale"),
                            rs.getBigDecimal("ca_total")));
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors du calcul des meilleures ventes", e);
        }
    }

    /** Valeur totale du stock (au prix d'achat) pour un depot donne : indicateur de gestion classique. */
    public BigDecimal valeurStockDepot(int idDepot) {
        authService.exigerPermission("RAPPORT_VOIR");
        String sql = """
                SELECT COALESCE(SUM(s.quantite * p.prix_achat_ht), 0) AS valeur
                FROM stocks s
                JOIN produits p ON p.id_produit = s.id_produit
                WHERE s.id_depot = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idDepot);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("valeur");
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors du calcul de la valeur du stock", e);
        }
    }
}
