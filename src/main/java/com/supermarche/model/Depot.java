package com.supermarche.model;

public class Depot {

    private Integer id;
    private String nom;
    private String adresse;
    private boolean pointDeVente = true;

    public Depot() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public boolean isPointDeVente() {
        return pointDeVente;
    }

    public void setPointDeVente(boolean pointDeVente) {
        this.pointDeVente = pointDeVente;
    }

    @Override
    public String toString() {
        return nom;
    }
}
