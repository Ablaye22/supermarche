package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.Categorie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategorieDao {

    public Categorie creer(Categorie categorie) {
        String sql = "INSERT INTO categories (nom, id_categorie_parente) VALUES (?, ?)";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categorie.getNom());
            if (categorie.getIdCategorieParente() != null) {
                ps.setInt(2, categorie.getIdCategorieParente());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    categorie.setId(rs.getInt(1));
                }
            }
            return categorie;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la creation de la categorie", e);
        }
    }

    public List<Categorie> listerToutes() {
        String sql = "SELECT * FROM categories ORDER BY nom";
        List<Categorie> resultats = new ArrayList<>();
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultats.add(mapper(rs));
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des categories", e);
        }
    }

    public Optional<Categorie> trouverParId(int id) {
        String sql = "SELECT * FROM categories WHERE id_categorie = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de la categorie id=" + id, e);
        }
    }

    private Categorie mapper(ResultSet rs) throws SQLException {
        Categorie c = new Categorie();
        c.setId(rs.getInt("id_categorie"));
        c.setNom(rs.getString("nom"));
        int idParente = rs.getInt("id_categorie_parente");
        c.setIdCategorieParente(rs.wasNull() ? null : idParente);
        return c;
    }
}
