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

    // id_sous_categorie reference categories.id_categorie (auto-reference) :
    // ce n'est PAS une jointure vers la table sous_categories, malgre le nom
    // trompeur de la colonne. On fait donc un self-join pour recuperer le
    // nom de la categorie "parente" quand elle existe.
    private static final String SELECT_BASE = """
            SELECT c.id_categorie, c.nom, c.id_sous_categorie, parent.nom AS nom_categorie_parente
            FROM categories c
            LEFT JOIN categories parent ON parent.id_categorie = c.id_sous_categorie
            """;

    public Categorie creer(Categorie categorie) {
        String sql = "INSERT INTO categories (nom, id_sous_categorie) VALUES (?, ?)";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categorie.getNom());
            if (categorie.getIdSousCategorie() != null) {
                ps.setInt(2, categorie.getIdSousCategorie());
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

    /**
     * Trie par nom de sous-categorie puis par nom de categorie, pour que
     * l'ecran Caisse puisse regrouper les boutons par section sans avoir
     * a retrier lui-meme la liste.
     */
    public List<Categorie> listerToutes() {
        String sql = SELECT_BASE + " ORDER BY COALESCE(parent.nom, c.nom), c.nom";
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
        String sql = SELECT_BASE + " WHERE c.id_categorie = ?";
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
        int idParente = rs.getInt("id_sous_categorie");
        c.setIdSousCategorie(rs.wasNull() ? null : idParente);
        c.setNomSousCategorie(rs.getString("nom_categorie_parente"));
        return c;
    }
}