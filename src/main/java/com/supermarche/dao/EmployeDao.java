package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.Employe;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeDao {

    public Employe creer(Employe employe) {
        String sql = """
                INSERT INTO employes (nom, prenom, email, telephone, date_embauche, actif)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employe.getNom());
            ps.setString(2, employe.getPrenom());
            ps.setString(3, employe.getEmail());
            ps.setString(4, employe.getTelephone());
            ps.setDate(5, employe.getDateEmbauche() != null ? Date.valueOf(employe.getDateEmbauche()) : null);
            ps.setBoolean(6, employe.isActif());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    employe.setId(rs.getInt(1));
                }
            }
            return employe;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la creation de l'employe", e);
        }
    }

    public void modifier(Employe employe) {
        String sql = """
                UPDATE employes SET nom = ?, prenom = ?, email = ?, telephone = ?,
                       date_embauche = ?, actif = ?
                WHERE id_employe = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, employe.getNom());
            ps.setString(2, employe.getPrenom());
            ps.setString(3, employe.getEmail());
            ps.setString(4, employe.getTelephone());
            ps.setDate(5, employe.getDateEmbauche() != null ? Date.valueOf(employe.getDateEmbauche()) : null);
            ps.setBoolean(6, employe.isActif());
            ps.setInt(7, employe.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la modification de l'employe id=" + employe.getId(), e);
        }
    }

    public Optional<Employe> trouverParId(int id) {
        String sql = "SELECT * FROM employes WHERE id_employe = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de l'employe id=" + id, e);
        }
    }

    public List<Employe> listerTous(boolean inclureInactifs) {
        String sql = "SELECT * FROM employes" + (inclureInactifs ? "" : " WHERE actif = TRUE") + " ORDER BY nom, prenom";
        List<Employe> resultats = new ArrayList<>();
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultats.add(mapper(rs));
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des employes", e);
        }
    }

    private Employe mapper(ResultSet rs) throws SQLException {
        Employe e = new Employe();
        e.setId(rs.getInt("id_employe"));
        e.setNom(rs.getString("nom"));
        e.setPrenom(rs.getString("prenom"));
        e.setEmail(rs.getString("email"));
        e.setTelephone(rs.getString("telephone"));
        Date dateEmbauche = rs.getDate("date_embauche");
        e.setDateEmbauche(dateEmbauche != null ? dateEmbauche.toLocalDate() : null);
        e.setActif(rs.getBoolean("actif"));
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        e.setDateCreation(dateCreation != null ? dateCreation.toLocalDateTime() : null);
        return e;
    }
}
