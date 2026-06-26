package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.SessionCaisse;
import com.supermarche.model.StatutSession;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class SessionCaisseDao {

    public SessionCaisse ouvrir(SessionCaisse session) {
        String sql = """
                INSERT INTO sessions_caisse (id_caisse, id_utilisateur, fond_ouverture, statut)
                VALUES (?, ?, ?, 'OUVERTE')
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, session.getIdCaisse());
            ps.setInt(2, session.getIdUtilisateur());
            ps.setBigDecimal(3, session.getFondOuverture());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    session.setId(rs.getLong(1));
                }
            }
            return session;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de l'ouverture de la session de caisse", e);
        }
    }

    public void fermer(long idSession, BigDecimal fondTheorique, BigDecimal fondReel, BigDecimal ecart) {
        String sql = """
                UPDATE sessions_caisse
                SET fond_fermeture_theorique = ?, fond_fermeture_reel = ?, ecart = ?,
                    date_fermeture = NOW(), statut = 'FERMEE'
                WHERE id_session = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBigDecimal(1, fondTheorique);
            ps.setBigDecimal(2, fondReel);
            ps.setBigDecimal(3, ecart);
            ps.setLong(4, idSession);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la fermeture de la session de caisse", e);
        }
    }

    public Optional<SessionCaisse> trouverSessionOuvertePourCaisse(int idCaisse) {
        String sql = """
                SELECT s.*, c.nom AS nom_caisse, u.nom_utilisateur
                FROM sessions_caisse s
                JOIN caisses c ON c.id_caisse = s.id_caisse
                JOIN utilisateurs u ON u.id_utilisateur = s.id_utilisateur
                WHERE s.id_caisse = ? AND s.statut = 'OUVERTE'
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCaisse);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de session ouverte", e);
        }
    }

    public Optional<SessionCaisse> trouverParId(long id) {
        String sql = """
                SELECT s.*, c.nom AS nom_caisse, u.nom_utilisateur
                FROM sessions_caisse s
                JOIN caisses c ON c.id_caisse = s.id_caisse
                JOIN utilisateurs u ON u.id_utilisateur = s.id_utilisateur
                WHERE s.id_session = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de la session id=" + id, e);
        }
    }

    /** Calcule le total des ventes validees encaissees en especes pour une session (pour le fond theorique). */
    public BigDecimal calculerTotalEspecesEncaisse(long idSession) {
        String sql = """
                SELECT COALESCE(SUM(pa.montant), 0) AS total
                FROM paiements pa
                JOIN ventes v ON v.id_vente = pa.id_vente
                WHERE v.id_session = ? AND v.statut = 'VALIDEE' AND pa.mode = 'ESPECES'
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, idSession);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors du calcul du total especes de la session", e);
        }
    }

    private SessionCaisse mapper(ResultSet rs) throws SQLException {
        SessionCaisse s = new SessionCaisse();
        s.setId(rs.getLong("id_session"));
        s.setIdCaisse(rs.getInt("id_caisse"));
        s.setNomCaisse(rs.getString("nom_caisse"));
        s.setIdUtilisateur(rs.getInt("id_utilisateur"));
        s.setNomUtilisateur(rs.getString("nom_utilisateur"));
        s.setFondOuverture(rs.getBigDecimal("fond_ouverture"));
        s.setFondFermetureTheorique(rs.getBigDecimal("fond_fermeture_theorique"));
        s.setFondFermetureReel(rs.getBigDecimal("fond_fermeture_reel"));
        s.setEcart(rs.getBigDecimal("ecart"));
        Timestamp ouverture = rs.getTimestamp("date_ouverture");
        s.setDateOuverture(ouverture != null ? ouverture.toLocalDateTime() : null);
        Timestamp fermeture = rs.getTimestamp("date_fermeture");
        s.setDateFermeture(fermeture != null ? fermeture.toLocalDateTime() : null);
        s.setStatut(StatutSession.valueOf(rs.getString("statut")));
        return s;
    }
}
