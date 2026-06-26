package com.supermarche.model;

import java.time.LocalDateTime;

public class Client {

    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String carteFidelite;
    private int pointsFidelite = 0;
    private LocalDateTime dateCreation;

    public Client() {
    }

    public String getNomComplet() {
        if (prenom == null || prenom.isBlank()) {
            return nom;
        }
        return prenom + " " + nom;
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

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getCarteFidelite() {
        return carteFidelite;
    }

    public void setCarteFidelite(String carteFidelite) {
        this.carteFidelite = carteFidelite;
    }

    public int getPointsFidelite() {
        return pointsFidelite;
    }

    public void setPointsFidelite(int pointsFidelite) {
        this.pointsFidelite = pointsFidelite;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return getNomComplet();
    }
}
