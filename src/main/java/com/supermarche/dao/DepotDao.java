package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.Depot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DepotDao {

    public List<Depot> listerTous() {
        String sql = "SELECT * FROM depots ORDER BY nom";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Depot> resultats = new ArrayList<>();
            while (rs.next()) {
                resultats.add(mapper(rs));
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des depots", e);
        }
    }

    public Optional<Depot> trouverParId(int id) {
        String sql = "SELECT * FROM depots WHERE id_depot = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche du depot id=" + id, e);
        }
    }

    private Depot mapper(ResultSet rs) throws SQLException {
        Depot d = new Depot();
        d.setId(rs.getInt("id_depot"));
        d.setNom(rs.getString("nom"));
        d.setAdresse(rs.getString("adresse"));
        d.setPointDeVente(rs.getBoolean("est_point_vente"));
        return d;
    }
}
