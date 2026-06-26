package com.supermarche.model;

import java.math.BigDecimal;

public class LigneCommandeFournisseur {

    private Long id;
    private Long idCommande;
    private Integer idProduit;
    private String designationProduit;
    private int quantiteCommandee;
    private int quantiteRecue = 0;
    private BigDecimal prixAchatUnitaire;

    public LigneCommandeFournisseur() {
    }

    public boolean estEntierementRecue() {
        return quantiteRecue >= quantiteCommandee;
    }

    public int getQuantiteRestante() {
        return Math.max(0, quantiteCommandee - quantiteRecue);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdCommande() {
        return idCommande;
    }

    public void setIdCommande(Long idCommande) {
        this.idCommande = idCommande;
    }

    public Integer getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(Integer idProduit) {
        this.idProduit = idProduit;
    }

    public String getDesignationProduit() {
        return designationProduit;
    }

    public void setDesignationProduit(String designationProduit) {
        this.designationProduit = designationProduit;
    }

    public int getQuantiteCommandee() {
        return quantiteCommandee;
    }

    public void setQuantiteCommandee(int quantiteCommandee) {
        this.quantiteCommandee = quantiteCommandee;
    }

    public int getQuantiteRecue() {
        return quantiteRecue;
    }

    public void setQuantiteRecue(int quantiteRecue) {
        this.quantiteRecue = quantiteRecue;
    }

    public BigDecimal getPrixAchatUnitaire() {
        return prixAchatUnitaire;
    }

    public void setPrixAchatUnitaire(BigDecimal prixAchatUnitaire) {
        this.prixAchatUnitaire = prixAchatUnitaire;
    }
}
