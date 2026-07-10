package com.supermarche.model;

public class Categorie {

    private Integer id;
    private String nom;
    private Integer idSousCategorie;
    private String nomSousCategorie;

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

    public Integer getIdSousCategorie() {
        return idSousCategorie;
    }

    public void setIdSousCategorie(Integer idSousCategorie) {
        this.idSousCategorie = idSousCategorie;
    }

    public String getNomSousCategorie() {
        return nomSousCategorie;
    }

    public void setNomSousCategorie(String nomSousCategorie) {
        this.nomSousCategorie = nomSousCategorie;
    }

    @Override
    public String toString() {
        return nom;
    }
}