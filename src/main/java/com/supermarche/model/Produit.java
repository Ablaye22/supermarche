package com.supermarche.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Produit {

    private Integer id;
    private String codeBarre;
    private String designation;
    private String description;
    private Categorie categorie;
    private Fournisseur fournisseurPrincipal;
    private BigDecimal prixAchatHt = BigDecimal.ZERO;
    private BigDecimal prixVenteTtc = BigDecimal.ZERO;
    private BigDecimal tauxTva = new BigDecimal("20.00");
    private String unite = "unite";
    private int seuilAlerteStock = 5;
    private boolean actif = true;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    public Produit() {
    }

    /** Marge brute unitaire en valeur (TTC vente - HT achat, indicatif). */
    public BigDecimal getMargeBrute() {
        return prixVenteTtc.subtract(prixAchatHt);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodeBarre() {
        return codeBarre;
    }

    public void setCodeBarre(String codeBarre) {
        this.codeBarre = codeBarre;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public Fournisseur getFournisseurPrincipal() {
        return fournisseurPrincipal;
    }

    public void setFournisseurPrincipal(Fournisseur fournisseurPrincipal) {
        this.fournisseurPrincipal = fournisseurPrincipal;
    }

    public BigDecimal getPrixAchatHt() {
        return prixAchatHt;
    }

    public void setPrixAchatHt(BigDecimal prixAchatHt) {
        this.prixAchatHt = prixAchatHt;
    }

    public BigDecimal getPrixVenteTtc() {
        return prixVenteTtc;
    }

    public void setPrixVenteTtc(BigDecimal prixVenteTtc) {
        this.prixVenteTtc = prixVenteTtc;
    }

    public BigDecimal getTauxTva() {
        return tauxTva;
    }

    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTva = tauxTva;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public int getSeuilAlerteStock() {
        return seuilAlerteStock;
    }

    public void setSeuilAlerteStock(int seuilAlerteStock) {
        this.seuilAlerteStock = seuilAlerteStock;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    @Override
    public String toString() {
        return designation;
    }
}
