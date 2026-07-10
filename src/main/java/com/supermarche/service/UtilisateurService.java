package com.supermarche.service;

import com.supermarche.dao.EmployeDao;
import com.supermarche.dao.RoleDao;
import com.supermarche.dao.UtilisateurDao;
import com.supermarche.exception.SupermarcheException;
import com.supermarche.model.CodeRole;
import com.supermarche.model.Employe;
import com.supermarche.model.Role;
import com.supermarche.model.Utilisateur;
import com.supermarche.security.PasswordHasher;

import java.util.List;

/**
 * Gestion administrative des comptes utilisateurs : creation, attribution
 * de role, activation/desactivation. Reservee aux titulaires de la
 * permission UTILISATEUR_GERER (role ADMIN par defaut).
 */
public class UtilisateurService {

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final EmployeDao employeDao = new EmployeDao();
    private final RoleDao roleDao = new RoleDao();
    private final AuthService authService;
    private final AuditService auditService = new AuditService();

    public UtilisateurService(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Cree un employe et son compte de connexion en une seule operation.
     * Le mot de passe initial est fourni en clair ici uniquement pour
     * etre immediatement hache ; il n'est jamais conserve ni journalise.
     */
    public Utilisateur creerUtilisateur(Employe employe, String nomUtilisateur, String motDePasseInitial, CodeRole codeRole) {
        authService.exigerPermission("UTILISATEUR_GERER");

        if (nomUtilisateur == null || nomUtilisateur.isBlank()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est obligatoire.");
        }
        if (utilisateurDao.trouverParNomUtilisateur(nomUtilisateur).isPresent()) {
            throw new SupermarcheException("Ce nom d'utilisateur est deja pris.");
        }

        Role role = roleDao.trouverParCode(codeRole)
                .orElseThrow(() -> new SupermarcheException("Role inconnu : " + codeRole));

        Employe employeCree = employeDao.creer(employe);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmploye(employeCree);
        utilisateur.setNomUtilisateur(nomUtilisateur);
        utilisateur.setMotDePasseHash(PasswordHasher.hacher(motDePasseInitial));
        utilisateur.setRole(role);
        utilisateur.setActif(true);

        Utilisateur cree = utilisateurDao.creer(utilisateur);
        auditService.enregistrer(idUtilisateurConnecte(), "UTILISATEUR_CREE", "utilisateurs",
                cree.getId().longValue(), "Role : " + codeRole);
        return cree;
    }

    public Utilisateur modifierUtilisateur(Utilisateur utilisateur, CodeRole codeRole){
        authService.exigerPermission("UTILISATEUR_GERER");

        if (utilisateur.getNomUtilisateur() == null || utilisateur.getNomUtilisateur().isBlank()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est obligatoire.");
        }

        Role role = roleDao.trouverParCode(codeRole).orElseThrow(() -> new SupermarcheException("Role inconnu : " + codeRole));

        utilisateur.setRole(role);
        utilisateur.setActif(true);

        Utilisateur cree = utilisateurDao.modifier(utilisateur);
        auditService.enregistrer(idUtilisateurConnecte(), "UTILISATEUR_MODIFIER", "utilisateurs",
                cree.getId().longValue(), "Role : " + codeRole);
        return cree;
    }

    public void activerOuDesactiver(int idUtilisateur, boolean actif) {
        authService.exigerPermission("UTILISATEUR_GERER");
        utilisateurDao.definirActif(idUtilisateur, actif);
        auditService.enregistrer(idUtilisateurConnecte(), actif ? "UTILISATEUR_ACTIVE" : "UTILISATEUR_DESACTIVE",
                "utilisateurs", (long) idUtilisateur, null);
    }

    public void deverrouillerCompte(int idUtilisateur) {
        authService.exigerPermission("UTILISATEUR_GERER");
        utilisateurDao.deverrouillerCompte(idUtilisateur);
        auditService.enregistrer(idUtilisateurConnecte(), "UTILISATEUR_DEVERROUILLE",
                "utilisateurs", (long) idUtilisateur, null);
    }

    public List<Utilisateur> listerTous() {
        authService.exigerPermission("UTILISATEUR_GERER");
        return utilisateurDao.listerTous();
    }

    public List<Role> listerRoles() {
        return roleDao.listerTous();
    }

    private Integer idUtilisateurConnecte() {
        return authService.getUtilisateurConnecte() != null ? authService.getUtilisateurConnecte().getId() : null;
    }
}
