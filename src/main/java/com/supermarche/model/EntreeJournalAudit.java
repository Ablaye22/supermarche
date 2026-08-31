package com.supermarche.model;

import java.time.LocalDateTime;

public class EntreeJournalAudit {

    private Long id;
    private Integer idUtilisateur;
    private String nomUtilisateur;
    private String action;
    private String entite;
    private Long idEntite;
    private String details;
    private String adresseIp;
    private LocalDateTime horodatage;

    public EntreeJournalAudit() {
    }

    public EntreeJournalAudit(Integer idUtilisateur, String action, String entite, Long idEntite, String details) {
        this.idUtilisateur = idUtilisateur;
        this.action = action;
        this.entite = entite;
        this.idEntite = idEntite;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Integer idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    public void setNomUtilisateur(String nomUtilisateur) {
        this.nomUtilisateur = nomUtilisateur;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntite() {
        return entite;
    }

    public void setEntite(String entite) {
        this.entite = entite;
    }

    public Long getIdEntite() {
        return idEntite;
    }

    public void setIdEntite(Long idEntite) {
        this.idEntite = idEntite;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getAdresseIp() {
        return adresseIp;
    }

    public void setAdresseIp(String adresseIp) {
        this.adresseIp = adresseIp;
    }

    public LocalDateTime getHorodatage() {
        return horodatage;
    }

    public void setHorodatage(LocalDateTime horodatage) {
        this.horodatage = horodatage;
    }
}
