package com.supermarche.controller;

import com.supermarche.model.Utilisateur;
import javafx.scene.control.TableColumn;

import com.supermarche.util.DialogueUtil;
import com.supermarche.config.ContexteApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


import javafx.scene.control.TableView;

public class PrelevementController{
    @FXML private TableView<Utilisateur> tableCaissier;
    @FXML private TableColumn<Utilisateur, String> colIdentifiant;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colStatus;

    @FXML
    public void initialize(){
        tableCaissier.getSelectionModel().selectedItemProperty().addListener((obs,ancien,nouveau)->{
            if(nouveau != null){
                chargerDansFormulaire(nouveau);
            }
        });
            }

     @FXML
    private void retourMenu() {
        PrincipalController.getInstanceCourante().afficherMenuAccueil();
    }

    @FXML
    private void encaissement(){
        
    }

    @FXML
    private void chargerDansFormulaire(Utilisateur u){

    }
}