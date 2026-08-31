package com.supermarche.model;
import java.time.LocalTime;

import java.time.LocalDate;

public class Planning {

    private int id;
    private Utilisateur employe;
    private LocalDate dateTravail;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private int pauseMinutes;
    private String poste;
    private String statut;
    private String observation;

    public Planning(){

    }
    public int getId(){
        return this.id;
    }
    public void setId(int id){
        this.id = id;
    }
    public Utilisateur getEmploye(){
        return this.employe;
    }
    public void setEmploye(Utilisateur e){
        this.employe = e;
    }
    public LocalDate getDate(){
        return dateTravail;
    }
    public void setDate(LocalDate d){
        this.dateTravail = d;
    }
    public LocalTime getHeureDebut(){
        return this.heureDebut;
    }
    public void setHeureDebut(LocalTime hd){
        this.heureDebut = hd;
    }
    public LocalTime getHeureFin(){
        return this.heureFin;
    }
    public void setHeureFin(LocalTime hf){
        this.heureFin = hf;
    }

    public int getPauseMinutes(){
       return  this.pauseMinutes;
    }
    public void setPauseMinutes(int pause){
        this.pauseMinutes = pause;
    }
    public String getPoste(){
        return this.poste;
    }
    public void setPoste(String poste){
        this.poste = poste;
    }
    public String getStatut(){
        return this.statut;
    }
    public void setStatut(String statut){
        this.statut = statut;
    }
    public String getObservation(){
        return this.observation;
    }
    public void setObservation(String observation){
        this.observation = observation;
    }
}