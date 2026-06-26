package com.supermarche.model;

public class Categorie {

    private Integer id;
    private String nom;
    private Integer idCategorieParente;

    public Categorie() {
    }

    public Categorie(Integer id, String nom) {
        this.id = id;
        this.nom = nom;
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

    public Integer getIdCategorieParente() {
        return idCategorieParente;
    }

    public void setIdCategorieParente(Integer idCategorieParente) {
        this.idCategorieParente = idCategorieParente;
    }

    @Override
    public String toString() {
        return nom;
    }
}
