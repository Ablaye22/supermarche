package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.Fournisseur;
import com.supermarche.util.DialogueUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class FournisseursController {

    @FXML private TableView<Fournisseur> tableFournisseurs;
    @FXML private TableColumn<Fournisseur, String> colRaisonSociale;
    @FXML private TableColumn<Fournisseur, String> colContact;
    @FXML private TableColumn<Fournisseur, String> colTelephone;
    @FXML private TableColumn<Fournisseur, String> colEmail;

    @FXML private Label labelTitreFormulaire;
    @FXML private TextField champRaisonSociale;
    @FXML private TextField champSiret;
    @FXML private TextField champContactNom;
    @FXML private TextField champTelephone;
    @FXML private TextField champEmail;
    @FXML private TextField champAdresse;

    private Fournisseur fournisseurSelectionne;

    @FXML
    public void initialize() {
        configurerColonnes();

        tableFournisseurs.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                chargerDansFormulaire(nouveau);
            }
        });

        chargerListe();
    }

    private void configurerColonnes() {
        colRaisonSociale.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRaisonSociale()));
        colContact.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getContactNom()));
        colTelephone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelephone()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
    }

    private void chargerListe() {
        List<Fournisseur> fournisseurs = ContexteApplication.getInstance().getFournisseurService().listerTous();
        tableFournisseurs.setItems(FXCollections.observableArrayList(fournisseurs));
    }

    @FXML
    private void nouveauFournisseur() {
        fournisseurSelectionne = null;
        labelTitreFormulaire.setText("Nouveau fournisseur");
        champRaisonSociale.clear();
        champSiret.clear();
        champContactNom.clear();
        champTelephone.clear();
        champEmail.clear();
        champAdresse.clear();
        tableFournisseurs.getSelectionModel().clearSelection();
    }

    private void chargerDansFormulaire(Fournisseur f) {
        fournisseurSelectionne = f;
        labelTitreFormulaire.setText("Modifier : " + f.getRaisonSociale());
        champRaisonSociale.setText(f.getRaisonSociale());
        champSiret.setText(f.getSiret());
        champContactNom.setText(f.getContactNom());
        champTelephone.setText(f.getTelephone());
        champEmail.setText(f.getEmail());
        champAdresse.setText(f.getAdresse());
    }

    @FXML
    private void enregistrerFournisseur() {
        try {
            Fournisseur f = (fournisseurSelectionne != null) ? fournisseurSelectionne : new Fournisseur();
            f.setRaisonSociale(champRaisonSociale.getText());
            f.setSiret(champSiret.getText());
            f.setContactNom(champContactNom.getText());
            f.setTelephone(champTelephone.getText());
            f.setEmail(champEmail.getText());
            f.setAdresse(champAdresse.getText());
            f.setActif(true);

            if (fournisseurSelectionne != null) {
                ContexteApplication.getInstance().getFournisseurService().modifierFournisseur(f);
            } else {
                ContexteApplication.getInstance().getFournisseurService().creerFournisseur(f);
            }
            chargerListe();
            nouveauFournisseur();
            DialogueUtil.afficherInfo("Fournisseur enregistre", "Les informations ont ete sauvegardees.");
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }
}
