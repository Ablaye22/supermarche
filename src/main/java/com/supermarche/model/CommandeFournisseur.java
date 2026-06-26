package com.supermarche.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommandeFournisseur {

    private Long id;
    private Integer idFournisseur;
    private String nomFournisseur;
    private StatutCommande statut = StatutCommande.BROUILLON;
    private LocalDate dateCommande;
    private LocalDate dateReceptionPrevue;
    private Integer idUtilisateur;
    private LocalDateTime dateCreation;
    private List<LigneCommandeFournisseur> lignes = new ArrayList<>();

    public CommandeFournisseur() {
    }

    public void ajouterLigne(LigneCommandeFournisseur ligne) {
        lignes.add(ligne);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdFournisseur() {
        return idFournisseur;
    }

    public void setIdFournisseur(Integer idFournisseur) {
        this.idFournisseur = idFournisseur;
    }

    public String getNomFournisseur() {
        return nomFournisseur;
    }

    public void setNomFournisseur(String nomFournisseur) {
        this.nomFournisseur = nomFournisseur;
    }

    public StatutCommande getStatut() {
        return statut;
    }

    public void setStatut(StatutCommande statut) {
        this.statut = statut;
    }

    public LocalDate getDateCommande() {
        return dateCommande;
    }

    public void setDateCommande(LocalDate dateCommande) {
        this.dateCommande = dateCommande;
    }

    public LocalDate getDateReceptionPrevue() {
        return dateReceptionPrevue;
    }

    public void setDateReceptionPrevue(LocalDate dateReceptionPrevue) {
        this.dateReceptionPrevue = dateReceptionPrevue;
    }

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Integer idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public List<LigneCommandeFournisseur> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneCommandeFournisseur> lignes) {
        this.lignes = lignes;
    }
}
