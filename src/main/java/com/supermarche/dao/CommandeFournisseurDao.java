package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.CommandeFournisseur;
import com.supermarche.model.LigneCommandeFournisseur;
import com.supermarche.model.StatutCommande;

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

public class CommandeFournisseurDao {

    public CommandeFournisseur creerAvecLignes(CommandeFournisseur commande) {
        String sqlCommande = """
                INSERT INTO commandes_fournisseur (id_fournisseur, statut, date_commande, date_reception_prevue, id_utilisateur)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection cnx = DatabaseConfig.getConnection()) {
            cnx.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cnx.prepareStatement(sqlCommande, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, commande.getIdFournisseur());
                    ps.setString(2, commande.getStatut().name());
                    ps.setDate(3, Date.valueOf(commande.getDateCommande()));
                    ps.setDate(4, commande.getDateReceptionPrevue() != null ? Date.valueOf(commande.getDateReceptionPrevue()) : null);
                    if (commande.getIdUtilisateur() != null) {
                        ps.setInt(5, commande.getIdUtilisateur());
                    } else {
                        ps.setNull(5, java.sql.Types.INTEGER);
                    }
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            commande.setId(rs.getLong(1));
                        }
                    }
                }

                String sqlLigne = """
                        INSERT INTO lignes_commande_fournisseur (id_commande, id_produit, quantite_commandee, prix_achat_unitaire)
                        VALUES (?, ?, ?, ?)
                        """;
                try (PreparedStatement ps = cnx.prepareStatement(sqlLigne)) {
                    for (LigneCommandeFournisseur ligne : commande.getLignes()) {
                        ps.setLong(1, commande.getId());
                        ps.setInt(2, ligne.getIdProduit());
                        ps.setInt(3, ligne.getQuantiteCommandee());
                        ps.setBigDecimal(4, ligne.getPrixAchatUnitaire());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                cnx.commit();
                return commande;
            } catch (Exception e) {
                cnx.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la creation de la commande fournisseur", e);
        }
    }

    public void mettreAJourStatut(long idCommande, StatutCommande statut) {
        try (Connection cnx = DatabaseConfig.getConnection()) {
            mettreAJourStatut(cnx, idCommande, statut);
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la mise a jour du statut de la commande", e);
        }
    }

    /** Variante utilisable a l'interieur d'une transaction externe (ex: reception de commande). */
    public void mettreAJourStatut(Connection cnx, long idCommande, StatutCommande statut) throws SQLException {
        String sql = "UPDATE commandes_fournisseur SET statut = ? WHERE id_commande = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setLong(2, idCommande);
            ps.executeUpdate();
        }
    }

    public void mettreAJourQuantiteRecue(Connection cnx, long idLigne, int quantiteRecue) throws SQLException {
        String sql = "UPDATE lignes_commande_fournisseur SET quantite_recue = ? WHERE id_ligne = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, quantiteRecue);
            ps.setLong(2, idLigne);
            ps.executeUpdate();
        }
    }

    public Optional<CommandeFournisseur> trouverParId(long id) {
        String sqlCommande = """
                SELECT cf.*, f.raison_sociale FROM commandes_fournisseur cf
                JOIN fournisseurs f ON f.id_fournisseur = cf.id_fournisseur
                WHERE cf.id_commande = ?
                """;
        try (Connection cnx = DatabaseConfig.getConnection()) {
            CommandeFournisseur commande;
            try (PreparedStatement ps = cnx.prepareStatement(sqlCommande)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    commande = mapperCommande(rs);
                }
            }
            commande.setLignes(chargerLignes(cnx, id));
            return Optional.of(commande);
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche de la commande id=" + id, e);
        }
    }

    public List<CommandeFournisseur> listerParStatut(StatutCommande statut) {
        String sql = """
                SELECT cf.*, f.raison_sociale FROM commandes_fournisseur cf
                JOIN fournisseurs f ON f.id_fournisseur = cf.id_fournisseur
                WHERE cf.statut = ?
                ORDER BY cf.date_commande DESC
                """;
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            List<CommandeFournisseur> resultats = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultats.add(mapperCommande(rs));
                }
            }
            return resultats;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des commandes par statut", e);
        }
    }

    private List<LigneCommandeFournisseur> chargerLignes(Connection cnx, long idCommande) throws SQLException {
        String sql = """
                SELECT lcf.*, p.designation FROM lignes_commande_fournisseur lcf
                JOIN produits p ON p.id_produit = lcf.id_produit
                WHERE lcf.id_commande = ?
                """;
        List<LigneCommandeFournisseur> lignes = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, idCommande);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LigneCommandeFournisseur l = new LigneCommandeFournisseur();
                    l.setId(rs.getLong("id_ligne"));
                    l.setIdCommande(rs.getLong("id_commande"));
                    l.setIdProduit(rs.getInt("id_produit"));
                    l.setDesignationProduit(rs.getString("designation"));
                    l.setQuantiteCommandee(rs.getInt("quantite_commandee"));
                    l.setQuantiteRecue(rs.getInt("quantite_recue"));
                    l.setPrixAchatUnitaire(rs.getBigDecimal("prix_achat_unitaire"));
                    lignes.add(l);
                }
            }
        }
        return lignes;
    }

    private CommandeFournisseur mapperCommande(ResultSet rs) throws SQLException {
        CommandeFournisseur c = new CommandeFournisseur();
        c.setId(rs.getLong("id_commande"));
        c.setIdFournisseur(rs.getInt("id_fournisseur"));
        c.setNomFournisseur(rs.getString("raison_sociale"));
        c.setStatut(StatutCommande.valueOf(rs.getString("statut")));
        Date dateCommande = rs.getDate("date_commande");
        c.setDateCommande(dateCommande != null ? dateCommande.toLocalDate() : null);
        Date dateReception = rs.getDate("date_reception_prevue");
        c.setDateReceptionPrevue(dateReception != null ? dateReception.toLocalDate() : null);
        int idUtilisateur = rs.getInt("id_utilisateur");
        c.setIdUtilisateur(rs.wasNull() ? null : idUtilisateur);
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        c.setDateCreation(dateCreation != null ? dateCreation.toLocalDateTime() : null);
        return c;
    }
}
