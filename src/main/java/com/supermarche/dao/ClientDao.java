package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientDao {

    public Client creer(Client c) {
        String sql = """
                INSERT INTO clients (nom, prenom, email, telephone, carte_fidelite, points_fidelite)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getPrenom());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getTelephone());
            ps.setString(5, c.getCarteFidelite());
            ps.setInt(6, c.getPointsFidelite());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    c.setId(rs.getInt(1));
                }
            }
            return c;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la creation du client", e);
        }
    }

    public void modifier(Client c) {
        String sql = """
                UPDATE clients SET nom = ?, prenom = ?, email = ?, telephone = ?,
                       carte_fidelite = ?, points_fidelite = ?
                WHERE id_client = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getPrenom());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getTelephone());
            ps.setString(5, c.getCarteFidelite());
            ps.setInt(6, c.getPointsFidelite());
            ps.setInt(7, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la modification du client id=" + c.getId(), e);
        }
    }

    /** Ajoute (ou retire si negatif) des points de fidelite, dans la meme transaction qu'une vente. */
    public void ajouterPointsFidelite(Connection cnx, int idClient, int pointsAAjouter) throws SQLException {
        String sql = "UPDATE clients SET points_fidelite = points_fidelite + ? WHERE id_client = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, pointsAAjouter);
            ps.setInt(2, idClient);
            ps.executeUpdate();
        }
    }

    public Optional<Client> trouverParId(int id) {
        String sql = "SELECT * FROM clients WHERE id_client = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche du client id=" + id, e);
        }
    }

    public Optional<Client> trouverParCarteFidelite(String carteFidelite) {
        String sql = "SELECT * FROM clients WHERE carte_fidelite = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, carteFidelite);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche du client par carte fidelite", e);
        }
    }

    public List<Client> rechercherParNom(String motCle) {
        String sql = "SELECT * FROM clients WHERE nom LIKE ? OR prenom LIKE ? ORDER BY nom LIMIT 100";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            String pattern = "%" + motCle + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            List<Client> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultats.add(mapper(rs));
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de clients", e);
        }
    }

    public List<Client> listerTous() {
        String sql = "SELECT * FROM clients ORDER BY nom, prenom";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Client> resultats = new ArrayList<>();
            while (rs.next()) {
                resultats.add(mapper(rs));
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des clients", e);
        }
    }

    private Client mapper(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setId(rs.getInt("id_client"));
        c.setNom(rs.getString("nom"));
        c.setPrenom(rs.getString("prenom"));
        c.setEmail(rs.getString("email"));
        c.setTelephone(rs.getString("telephone"));
        c.setCarteFidelite(rs.getString("carte_fidelite"));
        c.setPointsFidelite(rs.getInt("points_fidelite"));
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        c.setDateCreation(dateCreation != null ? dateCreation.toLocalDateTime() : null);
        return c;
    }
}
