package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.Fournisseur;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FournisseurDao {

    public Fournisseur creer(Fournisseur f) {
        String sql = """
                INSERT INTO fournisseurs (raison_sociale, siret, contact_nom, telephone, email, adresse, actif)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplirParametres(ps, f);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    f.setId(rs.getInt(1));
                }
            }
            return f;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la creation du fournisseur", e);
        }
    }

    public void modifier(Fournisseur f) {
        String sql = """
                UPDATE fournisseurs SET raison_sociale = ?, siret = ?, contact_nom = ?,
                       telephone = ?, email = ?, adresse = ?, actif = ?
                WHERE id_fournisseur = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            remplirParametres(ps, f);
            ps.setInt(8, f.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la modification du fournisseur id=" + f.getId(), e);
        }
    }

    private void remplirParametres(PreparedStatement ps, Fournisseur f) throws SQLException {
        ps.setString(1, f.getRaisonSociale());
        ps.setString(2, f.getSiret());
        ps.setString(3, f.getContactNom());
        ps.setString(4, f.getTelephone());
        ps.setString(5, f.getEmail());
        ps.setString(6, f.getAdresse());
        ps.setBoolean(7, f.isActif());
    }

    public List<Fournisseur> listerTous(boolean inclureInactifs) {
        String sql = "SELECT * FROM fournisseurs" + (inclureInactifs ? "" : " WHERE actif = TRUE") + " ORDER BY raison_sociale";
        List<Fournisseur> resultats = new ArrayList<>();
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultats.add(mapper(rs));
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des fournisseurs", e);
        }
    }

    public Optional<Fournisseur> trouverParId(int id) {
        String sql = "SELECT * FROM fournisseurs WHERE id_fournisseur = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche du fournisseur id=" + id, e);
        }
    }

    private Fournisseur mapper(ResultSet rs) throws SQLException {
        Fournisseur f = new Fournisseur();
        f.setId(rs.getInt("id_fournisseur"));
        f.setRaisonSociale(rs.getString("raison_sociale"));
        f.setSiret(rs.getString("siret"));
        f.setContactNom(rs.getString("contact_nom"));
        f.setTelephone(rs.getString("telephone"));
        f.setEmail(rs.getString("email"));
        f.setAdresse(rs.getString("adresse"));
        f.setActif(rs.getBoolean("actif"));
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        f.setDateCreation(dateCreation != null ? dateCreation.toLocalDateTime() : null);
        return f;
    }
}
