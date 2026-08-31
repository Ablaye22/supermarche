package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.Caisse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CaisseDao {

    public List<Caisse> listerActives() {
        String sql = "SELECT * FROM caisses WHERE active = TRUE ORDER BY nom";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Caisse> resultats = new ArrayList<>();
            while (rs.next()) {
                resultats.add(mapper(rs));
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des caisses", e);
        }
    }

    public Optional<Caisse> trouverParId(int id) {
        String sql = "SELECT * FROM caisses WHERE id_caisse = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de la caisse id=" + id, e);
        }
    }

    /** Indique si une session de caisse est deja ouverte (une seule session active autorisee par caisse). */
    public boolean possedeSessionOuverte(int idCaisse) {
        String sql = "SELECT COUNT(*) FROM sessions_caisse WHERE id_caisse = ? AND statut = 'OUVERTE'";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idCaisse);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la verification de session ouverte", e);
        }
    }

    private Caisse mapper(ResultSet rs) throws SQLException {
        Caisse c = new Caisse();
        c.setId(rs.getInt("id_caisse"));
        c.setNom(rs.getString("nom"));
        c.setIdDepot(rs.getInt("id_depot"));
        c.setActive(rs.getBoolean("active"));
        return c;
    }
}
