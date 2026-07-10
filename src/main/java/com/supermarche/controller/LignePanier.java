package com.supermarche.controller;

import com.supermarche.model.Produit;
// import javafx.beans.property.SimpleIntegerProperty;
// import javafx.beans.property.SimpleStringProperty;

import java.math.BigDecimal;

/**
 * Representation d'une ligne du panier cote IHM, avant qu'elle ne devienne
 * une LigneVente persistee. Separee du modele de domaine pour ne pas
 * polluer LigneVente (qui doit rester un simple reflet de la table SQL)
 * avec des proprietes JavaFX.
 */
public class LignePanier {

    private final Produit produit;
    private int quantite;

    public LignePanier(Produit produit, int quantite) {
        this.produit = produit;
        this.quantite = quantite;
    }

    public Produit getProduit() {
        return produit;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public String getDesignation() {
        return produit.getDesignation();
    }

    public BigDecimal getPrixUnitaire() {
        return produit.getPrixVenteTtc();
    }

    public BigDecimal getTotalLigne() {
        return produit.getPrixVenteTtc().multiply(BigDecimal.valueOf(quantite));
    }
}
