package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.CodeRole;
import com.supermarche.model.Employe;
import com.supermarche.model.Role;
import com.supermarche.model.Utilisateur;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UtilisateurDao {

    private final RoleDao roleDao = new RoleDao();

    public Utilisateur creer(Utilisateur utilisateur) {
        String sql = """
                INSERT INTO utilisateurs (id_employe, nom_utilisateur, mot_de_passe_hash, id_role, actif)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, utilisateur.getEmploye().getId());
            ps.setString(2, utilisateur.getNomUtilisateur());
            ps.setString(3, utilisateur.getMotDePasseHash());
            ps.setInt(4, utilisateur.getRole().getId());
            ps.setBoolean(5, utilisateur.isActif());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    utilisateur.setId(rs.getInt(1));
                }
            }
            return utilisateur;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la creation de l'utilisateur", e);
        }
    }

    public Optional<Utilisateur> trouverParNomUtilisateur(String nomUtilisateur) {
        String sql = """
                SELECT u.*, e.nom AS e_nom, e.prenom AS e_prenom, e.email AS e_email,
                       e.telephone AS e_telephone, e.date_embauche AS e_date_embauche, e.actif AS e_actif
                FROM utilisateurs u
                JOIN employes e ON e.id_employe = u.id_employe
                WHERE u.nom_utilisateur = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nomUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapperAvecEmploye(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de l'utilisateur " + nomUtilisateur, e);
        }
    }

    public Optional<Utilisateur> trouverParId(int id) {
        String sql = """
                SELECT u.*, e.nom AS e_nom, e.prenom AS e_prenom, e.email AS e_email,
                       e.telephone AS e_telephone, e.date_embauche AS e_date_embauche, e.actif AS e_actif
                FROM utilisateurs u
                JOIN employes e ON e.id_employe = u.id_employe
                WHERE u.id_utilisateur = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapperAvecEmploye(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de l'utilisateur id=" + id, e);
        }
    }

    public List<Utilisateur> listerTous() {
        String sql = """
                SELECT u.*, e.nom AS e_nom, e.prenom AS e_prenom, e.email AS e_email,
                       e.telephone AS e_telephone, e.date_embauche AS e_date_embauche, e.actif AS e_actif
                FROM utilisateurs u
                JOIN employes e ON e.id_employe = u.id_employe
                ORDER BY e.nom, e.prenom
                """;
        List<Utilisateur> resultats = new ArrayList<>();
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultats.add(mapperAvecEmploye(rs));
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des utilisateurs", e);
        }
    }

    /** Incremente le compteur d'echecs de connexion et verrouille le compte si le seuil est atteint. */
    public void incrementerTentativesEchouees(int idUtilisateur, int seuilVerrouillage) {
        String sql = """
                UPDATE utilisateurs
                SET tentatives_echouees = tentatives_echouees + 1,
                    compte_verrouille = (tentatives_echouees + 1 >= ?)
                WHERE id_utilisateur = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, seuilVerrouillage);
            ps.setInt(2, idUtilisateur);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la mise a jour des tentatives echouees", e);
        }
    }

    /** Reinitialise le compteur d'echecs et enregistre la date de derniere connexion reussie. */
    public void enregistrerConnexionReussie(int idUtilisateur) {
        String sql = """
                UPDATE utilisateurs
                SET tentatives_echouees = 0, derniere_connexion = NOW()
                WHERE id_utilisateur = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de l'enregistrement de la connexion", e);
        }
    }

    public void deverrouillerCompte(int idUtilisateur) {
        String sql = "UPDATE utilisateurs SET compte_verrouille = FALSE, tentatives_echouees = 0 WHERE id_utilisateur = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idUtilisateur);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors du deverrouillage du compte", e);
        }
    }

    public void changerMotDePasse(int idUtilisateur, String nouveauHash) {
        String sql = "UPDATE utilisateurs SET mot_de_passe_hash = ? WHERE id_utilisateur = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauHash);
            ps.setInt(2, idUtilisateur);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors du changement de mot de passe", e);
        }
    }

    public void definirActif(int idUtilisateur, boolean actif) {
        String sql = "UPDATE utilisateurs SET actif = ? WHERE id_utilisateur = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBoolean(1, actif);
            ps.setInt(2, idUtilisateur);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la mise a jour du statut actif", e);
        }
    }

    private Utilisateur mapperAvecEmploye(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id_utilisateur"));
        u.setNomUtilisateur(rs.getString("nom_utilisateur"));
        u.setMotDePasseHash(rs.getString("mot_de_passe_hash"));
        u.setActif(rs.getBoolean("actif"));
        u.setCompteVerrouille(rs.getBoolean("compte_verrouille"));
        u.setTentativesEchouees(rs.getInt("tentatives_echouees"));
        Timestamp derniereConnexion = rs.getTimestamp("derniere_connexion");
        u.setDerniereConnexion(derniereConnexion != null ? derniereConnexion.toLocalDateTime() : null);
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        u.setDateCreation(dateCreation != null ? dateCreation.toLocalDateTime() : null);

        Employe employe = new Employe();
        employe.setId(rs.getInt("id_employe"));
        employe.setNom(rs.getString("e_nom"));
        employe.setPrenom(rs.getString("e_prenom"));
        employe.setEmail(rs.getString("e_email"));
        employe.setTelephone(rs.getString("e_telephone"));
        employe.setActif(rs.getBoolean("e_actif"));
        u.setEmploye(employe);

        Role role = roleDao.trouverParId(rs.getInt("id_role")).orElse(null);
        u.setRole(role);

        return u;
    }
}
