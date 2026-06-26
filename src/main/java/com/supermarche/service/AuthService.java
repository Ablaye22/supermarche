package com.supermarche.service;

import com.supermarche.config.AppConfig;
import com.supermarche.dao.UtilisateurDao;
import com.supermarche.exception.AuthentificationException;
import com.supermarche.exception.AutorisationException;
import com.supermarche.model.Utilisateur;
import com.supermarche.security.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Gere l'authentification et maintient l'utilisateur actuellement connecte
 * sur ce poste (une seule session locale a la fois, ce qui correspond a
 * l'usage reel : un caissier se connecte sur le poste physique devant lui).
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UtilisateurDao utilisateurDao;
    private final AuditService auditService;

    private Utilisateur utilisateurConnecte;

    public AuthService() {
        this(new UtilisateurDao(), new AuditService());
    }

    /** Constructeur utilise par les tests pour injecter des DAO/services simules. */
    public AuthService(UtilisateurDao utilisateurDao, AuditService auditService) {
        this.utilisateurDao = utilisateurDao;
        this.auditService = auditService;
    }

    /**
     * Authentifie un utilisateur. Ne distingue jamais "nom d'utilisateur
     * inconnu" de "mot de passe incorrect" dans le message renvoye, pour
     * ne pas reveler a un attaquant quels comptes existent.
     */
    public Utilisateur seConnecter(String nomUtilisateur, String motDePasseClair) {
        Optional<Utilisateur> resultat = utilisateurDao.trouverParNomUtilisateur(nomUtilisateur);

        if (resultat.isEmpty()) {
            // On enregistre tout de meme une tentative dans le journal, sans id utilisateur,
            // pour detecter d'eventuelles attaques par enumeration de comptes.
            auditService.enregistrer(null, "CONNEXION_ECHOUEE", "utilisateurs", null,
                    "Nom d'utilisateur inconnu : " + nomUtilisateur);
            throw new AuthentificationException("Identifiant ou mot de passe incorrect.");
        }

        Utilisateur utilisateur = resultat.get();

        if (utilisateur.isCompteVerrouille()) {
            auditService.enregistrer(utilisateur.getId(), "CONNEXION_REFUSEE_VERROUILLE");
            throw new AuthentificationException(
                    "Ce compte est verrouille suite a plusieurs echecs de connexion. Contactez un administrateur.");
        }

        if (!utilisateur.isActif()) {
            auditService.enregistrer(utilisateur.getId(), "CONNEXION_REFUSEE_INACTIF");
            throw new AuthentificationException("Ce compte est desactive.");
        }

        boolean motDePasseValide = PasswordHasher.verifier(motDePasseClair, utilisateur.getMotDePasseHash());

        if (!motDePasseValide) {
            int seuil = AppConfig.getInstance().getMaxLoginAttempts();
            utilisateurDao.incrementerTentativesEchouees(utilisateur.getId(), seuil);
            auditService.enregistrer(utilisateur.getId(), "CONNEXION_ECHOUEE", "utilisateurs", utilisateur.getId().longValue(), null);
            throw new AuthentificationException("Identifiant ou mot de passe incorrect.");
        }

        utilisateurDao.enregistrerConnexionReussie(utilisateur.getId());
        auditService.enregistrer(utilisateur.getId(), "CONNEXION_REUSSIE");
        this.utilisateurConnecte = utilisateur;
        log.info("Connexion reussie : {}", nomUtilisateur);
        return utilisateur;
    }

    public void seDeconnecter() {
        if (utilisateurConnecte != null) {
            auditService.enregistrer(utilisateurConnecte.getId(), "DECONNEXION");
        }
        this.utilisateurConnecte = null;
    }

    public Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public boolean estConnecte() {
        return utilisateurConnecte != null;
    }

    /**
     * Verifie que l'utilisateur connecte possede la permission demandee.
     * A appeler en debut de chaque operation sensible dans les services
     * metier, pas seulement au niveau de l'IHM : un controle uniquement
     * dans l'IHM peut etre contourne en appelant directement le service.
     */
    public void exigerPermission(String codePermission) {
        if (!estConnecte()) {
            throw new AuthentificationException("Aucun utilisateur connecte.");
        }
        if (!utilisateurConnecte.possedePermission(codePermission)) {
            auditService.enregistrer(utilisateurConnecte.getId(), "ACCES_REFUSE", "permission", null, codePermission);
            throw new AutorisationException("Action non autorisee pour votre role : " + codePermission);
        }
    }

    /** Change le mot de passe de l'utilisateur connecte, apres verification de l'ancien mot de passe. */
    public void changerMonMotDePasse(String ancienMotDePasse, String nouveauMotDePasse) {
        if (!estConnecte()) {
            throw new AuthentificationException("Aucun utilisateur connecte.");
        }
        if (!PasswordHasher.verifier(ancienMotDePasse, utilisateurConnecte.getMotDePasseHash())) {
            throw new AuthentificationException("L'ancien mot de passe est incorrect.");
        }
        validerForceMotDePasse(nouveauMotDePasse);
        String nouveauHash = PasswordHasher.hacher(nouveauMotDePasse);
        utilisateurDao.changerMotDePasse(utilisateurConnecte.getId(), nouveauHash);
        utilisateurConnecte.setMotDePasseHash(nouveauHash);
        auditService.enregistrer(utilisateurConnecte.getId(), "MOT_DE_PASSE_CHANGE");
    }

    /** Reinitialisation par un administrateur (sans connaitre l'ancien mot de passe). */
    public void reinitialiserMotDePasse(int idUtilisateurCible, String nouveauMotDePasse) {
        exigerPermission("UTILISATEUR_GERER");
        validerForceMotDePasse(nouveauMotDePasse);
        String nouveauHash = PasswordHasher.hacher(nouveauMotDePasse);
        utilisateurDao.changerMotDePasse(idUtilisateurCible, nouveauHash);
        utilisateurDao.deverrouillerCompte(idUtilisateurCible);
        auditService.enregistrer(utilisateurConnecte.getId(), "MOT_DE_PASSE_REINITIALISE",
                "utilisateurs", (long) idUtilisateurCible, null);
    }

    private void validerForceMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.length() < 10) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 10 caracteres.");
        }
        boolean aMajuscule = motDePasse.chars().anyMatch(Character::isUpperCase);
        boolean aMinuscule = motDePasse.chars().anyMatch(Character::isLowerCase);
        boolean aChiffre = motDePasse.chars().anyMatch(Character::isDigit);
        if (!aMajuscule || !aMinuscule || !aChiffre) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre.");
        }
    }
}
