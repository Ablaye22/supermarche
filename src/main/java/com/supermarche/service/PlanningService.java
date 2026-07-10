package com.supermarche.service;

import java.time.LocalDate;
import java.util.List;
import com.supermarche.dao.PlanningDao;
import com.supermarche.exception.ValidationException;
import com.supermarche.model.Planning;

public class PlanningService {

    private final PlanningDao planningDao = new PlanningDao();
    private final AuthService authService;
    private final AuditService auditService = new AuditService();

    public PlanningService(AuthService authService){
        this.authService = authService;
    }

    public Planning creerPlanning(Planning planning) {
        valider(planning);

        Planning cree = planningDao.creer(planning);
        return cree;
    }

    public void modifierPlanning(Planning planning) {

        if (planning.getId() <= 0) {
            throw new ValidationException("Planning introuvable.");
        }

        valider(planning);

        planningDao.modifier(planning);
    }

    public void supprimerPlanning(int id) {

        planningDao.supprimer(id);

    }

    public List<Planning> trouverParId(int id) {

        return planningDao.trouverParId(id);

    }

    public List<Planning> listerTous() {

        return planningDao.listerTous();

    }

    public List<Planning> listerParEmploye(int idEmploye) {

        return planningDao.listerParEmploye(idEmploye);

    }

    public List<Planning> listerParDate(LocalDate date) {

        return planningDao.listerParDate(date);

    }

    public List<Planning> listerEntreDates(LocalDate debut, LocalDate fin) {

        return planningDao.listerEntreDates(debut, fin);

    }

    private void valider(Planning planning) {

        if (planning == null) {
            throw new ValidationException("Planning invalide.");
        }

        if (planning.getEmploye() == null) {
            throw new ValidationException("Veuillez sélectionner un employé.");
        }

        if (planning.getDate() == null) {
            throw new ValidationException("Veuillez sélectionner une date.");
        }

        if (planning.getHeureDebut() == null) {
            throw new ValidationException("Veuillez saisir l'heure de début.");
        }

        if (planning.getHeureFin() == null) {
            throw new ValidationException("Veuillez saisir l'heure de fin.");
        }

        if (planning.getHeureFin().isBefore(planning.getHeureDebut())) {
            throw new ValidationException(
                    "L'heure de fin doit être après l'heure de début.");
        }

        if (planning.getPauseMinutes() < 0) {
            throw new ValidationException(
                    "La durée de la pause est invalide.");
        }

        if (planning.getPoste() == null ||
                planning.getPoste().isBlank()) {

            throw new ValidationException(
                    "Veuillez saisir le poste.");
        }

        if (planning.getStatut() == null ||
                planning.getStatut().isBlank()) {

            throw new ValidationException(
                    "Veuillez sélectionner un statut.");
        }

    }
    private Integer idUtilisateurConnecte() {
        return authService.getUtilisateurConnecte() != null ? authService.getUtilisateurConnecte().getId() : null;
    }
}