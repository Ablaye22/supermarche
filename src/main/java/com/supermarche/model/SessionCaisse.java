package com.supermarche.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represente l'ouverture/fermeture d'une caisse par un caissier.
 * Permet le controle des ecarts entre le fond theorique (calcule a partir
 * des ventes) et le fond reel compte physiquement a la fermeture.
 */
public class SessionCaisse {

    private Long id;
    private Integer idCaisse;
    private String nomCaisse;
    private Integer idUtilisateur;
    private String nomUtilisateur;
    private BigDecimal fondOuverture;
    private BigDecimal fondFermetureTheorique;
    private BigDecimal fondFermetureReel;
    private BigDecimal ecart;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateFermeture;
    private StatutSession statut = StatutSession.OUVERTE;

    public SessionCaisse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdCaisse() {
        return idCaisse;
    }

    public void setIdCaisse(Integer idCaisse) {
        this.idCaisse = idCaisse;
    }

    public String getNomCaisse() {
        return nomCaisse;
    }

    public void setNomCaisse(String nomCaisse) {
        this.nomCaisse = nomCaisse;
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

    public BigDecimal getFondOuverture() {
        return fondOuverture;
    }

    public void setFondOuverture(BigDecimal fondOuverture) {
        this.fondOuverture = fondOuverture;
    }

    public BigDecimal getFondFermetureTheorique() {
        return fondFermetureTheorique;
    }

    public void setFondFermetureTheorique(BigDecimal fondFermetureTheorique) {
        this.fondFermetureTheorique = fondFermetureTheorique;
    }

    public BigDecimal getFondFermetureReel() {
        return fondFermetureReel;
    }

    public void setFondFermetureReel(BigDecimal fondFermetureReel) {
        this.fondFermetureReel = fondFermetureReel;
    }

    public BigDecimal getEcart() {
        return ecart;
    }

    public void setEcart(BigDecimal ecart) {
        this.ecart = ecart;
    }

    public LocalDateTime getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(LocalDateTime dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public LocalDateTime getDateFermeture() {
        return dateFermeture;
    }

    public void setDateFermeture(LocalDateTime dateFermeture) {
        this.dateFermeture = dateFermeture;
    }

    public StatutSession getStatut() {
        return statut;
    }

    public void setStatut(StatutSession statut) {
        this.statut = statut;
    }
}
