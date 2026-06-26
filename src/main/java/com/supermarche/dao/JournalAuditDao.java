package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.EntreeJournalAudit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JournalAuditDao {

    /** Insertion volontairement "best effort" : voir AuditService pour la justification. */
    public void enregistrer(EntreeJournalAudit entree) {
        String sql = """
                INSERT INTO journal_audit (id_utilisateur, action, entite, id_entite, details, adresse_ip)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            if (entree.getIdUtilisateur() != null) {
                ps.setInt(1, entree.getIdUtilisateur());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, entree.getAction());
            ps.setString(3, entree.getEntite());
            if (entree.getIdEntite() != null) {
                ps.setLong(4, entree.getIdEntite());
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setString(5, entree.getDetails());
            ps.setString(6, entree.getAdresseIp());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de l'ecriture du journal d'audit", e);
        }
    }

    public List<EntreeJournalAudit> rechercher(LocalDateTime depuis, LocalDateTime jusqua,
                                                Integer idUtilisateur, String action, int limite) {
        StringBuilder sql = new StringBuilder("""
                SELECT j.*, u.nom_utilisateur
                FROM journal_audit j
                LEFT JOIN utilisateurs u ON u.id_utilisateur = j.id_utilisateur
                WHERE j.horodatage BETWEEN ? AND ?
                """);
        if (idUtilisateur != null) {
            sql.append(" AND j.id_utilisateur = ?");
        }
        if (action != null && !action.isBlank()) {
            sql.append(" AND j.action = ?");
        }
        sql.append(" ORDER BY j.horodatage DESC LIMIT ?");

        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setTimestamp(index++, Timestamp.valueOf(depuis));
            ps.setTimestamp(index++, Timestamp.valueOf(jusqua));
            if (idUtilisateur != null) {
                ps.setInt(index++, idUtilisateur);
            }
            if (action != null && !action.isBlank()) {
                ps.setString(index++, action);
            }
            ps.setInt(index, limite);

            List<EntreeJournalAudit> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultats.add(mapper(rs));
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche dans le journal d'audit", e);
        }
    }

    private EntreeJournalAudit mapper(ResultSet rs) throws SQLException {
        EntreeJournalAudit entree = new EntreeJournalAudit();
        entree.setId(rs.getLong("id_journal"));
        int idUtilisateur = rs.getInt("id_utilisateur");
        entree.setIdUtilisateur(rs.wasNull() ? null : idUtilisateur);
        entree.setNomUtilisateur(rs.getString("nom_utilisateur"));
        entree.setAction(rs.getString("action"));
        entree.setEntite(rs.getString("entite"));
        long idEntite = rs.getLong("id_entite");
        entree.setIdEntite(rs.wasNull() ? null : idEntite);
        entree.setDetails(rs.getString("details"));
        entree.setAdresseIp(rs.getString("adresse_ip"));
        entree.setHorodatage(rs.getTimestamp("horodatage").toLocalDateTime());
        return entree;
    }
}
