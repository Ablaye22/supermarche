package com.supermarche.model;

import java.time.LocalDateTime;

/**
 * Compte de connexion d'un employe. Volontairement separe de l'entite
 * Employe : un employe peut exister sans avoir de compte (ex: livreur),
 * et cela isole les donnees d'authentification sensibles.
 */
public class Utilisateur {

    private Integer id;
    private Employe employe;
    private String nomUtilisateur;

    /** Hash BCrypt uniquement. Le mot de passe en clair ne doit JAMAIS transiter au-dela de la saisie. */
    private String motDePasseHash;

    private Role role;
    private boolean actif = true;
    private boolean compteVerrouille = false;
    private int tentativesEchouees = 0;
    private LocalDateTime derniereConnexion;
    private LocalDateTime dateCreation;

    public Utilisateur() {
    }

    public boolean possedePermission(String codePermission) {
        return role != null && role.possedePermission(codePermission);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Employe getEmploye() {
        return employe;
    }

    public void setEmploye(Employe employe) {
        this.employe = employe;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    public void setNomUtilisateur(String nomUtilisateur) {
        this.nomUtilisateur = nomUtilisateur;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public boolean isCompteVerrouille() {
        return compteVerrouille;
    }

    public void setCompteVerrouille(boolean compteVerrouille) {
        this.compteVerrouille = compteVerrouille;
    }

    public int getTentativesEchouees() {
        return tentativesEchouees;
    }

    public void setTentativesEchouees(int tentativesEchouees) {
        this.tentativesEchouees = tentativesEchouees;
    }

    public LocalDateTime getDerniereConnexion() {
        return derniereConnexion;
    }

    public void setDerniereConnexion(LocalDateTime derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
