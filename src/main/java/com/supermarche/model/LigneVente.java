package com.supermarche.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ligne d'un ticket de vente. Le prix unitaire et le taux de TVA sont
 * copies depuis le produit AU MOMENT DE LA VENTE (historisation) :
 * si le prix du produit change plus tard, les anciens tickets ne doivent
 * pas etre affectes.
 */
public class LigneVente {

    private Long id;
    private Long idVente;
    private Integer idProduit;
    private String designationProduit;
    private BigDecimal quantite;
    private BigDecimal prixUnitaireTtc;
    private BigDecimal tauxTva;
    private BigDecimal remisePourcentage = BigDecimal.ZERO;
    private BigDecimal totalLigneTtc;

    public LigneVente() {
    }

    public LigneVente(Integer idProduit, String designationProduit, BigDecimal quantite,
                       BigDecimal prixUnitaireTtc, BigDecimal tauxTva) {
        this.idProduit = idProduit;
        this.designationProduit = designationProduit;
        this.quantite = quantite;
        this.prixUnitaireTtc = prixUnitaireTtc;
        this.tauxTva = tauxTva;
        recalculerTotal();
    }

    /** Recalcule le total de la ligne en appliquant la remise eventuelle. */
    public void recalculerTotal() {
        BigDecimal brut = prixUnitaireTtc.multiply(quantite);
        BigDecimal facteurRemise = BigDecimal.ONE.subtract(
                remisePourcentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        this.totalLigneTtc = brut.multiply(facteurRemise).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdVente() {
        return idVente;
    }

    public void setIdVente(Long idVente) {
        this.idVente = idVente;
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

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaireTtc() {
        return prixUnitaireTtc;
    }

    public void setPrixUnitaireTtc(BigDecimal prixUnitaireTtc) {
        this.prixUnitaireTtc = prixUnitaireTtc;
    }

    public BigDecimal getTauxTva() {
        return tauxTva;
    }

    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTva = tauxTva;
    }

    public BigDecimal getRemisePourcentage() {
        return remisePourcentage;
    }

    public void setRemisePourcentage(BigDecimal remisePourcentage) {
        this.remisePourcentage = remisePourcentage;
    }

    public BigDecimal getTotalLigneTtc() {
        return totalLigneTtc;
    }

    public void setTotalLigneTtc(BigDecimal totalLigneTtc) {
        this.totalLigneTtc = totalLigneTtc;
    }
}
