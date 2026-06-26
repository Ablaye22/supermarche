package com.supermarche.model;

public class Stock {

    private Integer idProduit;
    private Integer idDepot;
    private String designationProduit; // pratique pour l'affichage (jointure)
    private int quantite;

    public Stock() {
    }

    public Stock(Integer idProduit, Integer idDepot, int quantite) {
        this.idProduit = idProduit;
        this.idDepot = idDepot;
        this.quantite = quantite;
    }

    public Integer getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(Integer idProduit) {
        this.idProduit = idProduit;
    }

    public Integer getIdDepot() {
        return idDepot;
    }

    public void setIdDepot(Integer idDepot) {
        this.idDepot = idDepot;
    }

    public String getDesignationProduit() {
        return designationProduit;
    }

    public void setDesignationProduit(String designationProduit) {
        this.designationProduit = designationProduit;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }
}
