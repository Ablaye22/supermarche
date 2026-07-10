package com.supermarche.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.Utilisateur;
import com.supermarche.model.Employe;
import com.supermarche.model.Planning;

public class PlanningDao {

    public Planning creer(Planning planning) {
        String sql = """
            INSERT INTO planning(
                id_employe,
                date_travail,
                heure_debut,
                heure_fin,
                pause_minutes,
                poste,
                statut,
                observation
            )
            VALUES(?,?,?,?,?,?,?,?)
            """;

        try(Connection cnx = DatabaseConfig.getConnection();
            PreparedStatement ps =
                    cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){

            remplirParametres(ps, planning);

            ps.executeUpdate();

            try(ResultSet rs = ps.getGeneratedKeys()){

                if(rs.next()){
                    planning.setId(rs.getInt(1));
                }

            }

            return planning;

        }catch(SQLException e){

            throw new AccesDonneesException(
                    "Erreur lors de la création du planning",
                    e
            );

        }

    }

    public void modifier(Planning planning){

        String sql="""
            UPDATE planning
            SET
                id_employe=?,
                date_travail=?,
                heure_debut=?,
                heure_fin=?,
                pause_minutes=?,
                poste=?,
                statut=?,
                observation=?
            WHERE id_planning=?
            """;

        try(Connection cnx=DatabaseConfig.getConnection();
            PreparedStatement ps=cnx.prepareStatement(sql)){

            remplirParametres(ps,planning);

            ps.setInt(9,planning.getId());

            ps.executeUpdate();

        }catch(SQLException e){

            throw new AccesDonneesException(
                    "Erreur modification planning",
                    e
            );

        }

    }

    public void supprimer(int id){

        String sql="DELETE FROM planning WHERE id_planning=?";

        try(Connection cnx=DatabaseConfig.getConnection();
            PreparedStatement ps=cnx.prepareStatement(sql)){

            ps.setInt(1,id);

            ps.executeUpdate();

        }catch(SQLException e){

            throw new AccesDonneesException(
                    "Erreur suppression planning",
                    e
            );

        }

    }
    private void remplirParametres(
            PreparedStatement ps,
            Planning planning
    ) throws SQLException{

        ps.setInt(1, planning.getEmploye().getEmploye().getId());
        ps.setDate(2, java.sql.Date.valueOf(planning.getDate()));
        ps.setTime(3, java.sql.Time.valueOf(planning.getHeureDebut()));
        ps.setTime(4, java.sql.Time.valueOf(planning.getHeureFin()));
        ps.setInt(5, planning.getPauseMinutes());
        ps.setString(6, planning.getPoste());
        ps.setString(7, planning.getStatut());
        ps.setString(8, planning.getObservation());
    }

    public List<Planning> trouverParId(int id) {
        String sql = baseSelect() + " WHERE p.id_planning = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? List.of(mapper(rs)) : List.of();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche du planning id=" + id, e);
        }
    }

    public List<Planning> listerParEmploye(int idEmploye) {

        String sql = baseSelect() + " WHERE p.id_employe = ? ORDER BY date_travail DESC";
        List<Planning> liste = new ArrayList<>();
        try (Connection cnx = DatabaseConfig.getConnection();
                PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEmploye);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapper(rs));
                }
            }
            return liste;

        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche des plannings", e);
        }
    }

    public List<Planning> listerTous(){
         String sql = baseSelect() + " ORDER BY date_travail DESC";
        List<Planning> liste = new ArrayList<>();
        try (Connection cnx = DatabaseConfig.getConnection();
                PreparedStatement ps = cnx.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapper(rs));
                }
            }
            return liste;

        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche des plannings", e);
        }
    }

    public List<Planning> listerParDate(LocalDate date){
         String sql = baseSelect() +
                " WHERE p.date_travail = ? ";

        List<Planning> liste = new ArrayList<>();

        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    liste.add(mapper(rs));
                }

            }

            return liste;

        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche des plannings", e);
        }
    }

    public List<Planning> listerEntreDates(LocalDate debut, LocalDate fin) {

        String sql = baseSelect() +
                " WHERE p.date_travail BETWEEN ? AND ? ORDER BY date_travail, heure_debut";

        List<Planning> liste = new ArrayList<>();

        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(debut));
            ps.setDate(2, Date.valueOf(fin));

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    liste.add(mapper(rs));
                }

            }

            return liste;

        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche des plannings", e);
        }
    }
     private String baseSelect() {

        return """
                SELECT p.*, e.id_employe, e.nom, e.prenom
                FROM planning p
                JOIN employes e
                    ON p.id_employe = e.id_employe
                """;
    }

    private Planning mapper(ResultSet rs)
            throws SQLException{

        Planning planning = new Planning();
        planning.setId(rs.getInt("id_planning"));

        Employe employe = new Employe();
        employe.setId(rs.getInt("id_employe"));
        employe.setNom(rs.getString("nom"));
        employe.setPrenom(rs.getString("prenom"));

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmploye(employe);
        planning.setEmploye(utilisateur);

        planning.setDate(rs.getDate("date_travail").toLocalDate());
        planning.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        planning.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        planning.setPauseMinutes(rs.getInt("pause_minutes"));
        planning.setPoste(rs.getString("poste"));
        planning.setStatut(rs.getString("statut"));
        planning.setObservation(rs.getString("observation"));
        return planning;

    }
}