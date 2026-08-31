package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.Client;
import com.supermarche.util.DialogueUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class ClientsController {

    @FXML private TextField champRecherche;
    @FXML private TableView<Client> tableClients;
    @FXML private TableColumn<Client, String> colNomComplet;
    @FXML private TableColumn<Client, String> colTelephone;
    @FXML private TableColumn<Client, String> colCarteFidelite;
    @FXML private TableColumn<Client, String> colPoints;

    @FXML private Label labelTitreFormulaire;
    @FXML private TextField champNom;
    @FXML private TextField champPrenom;
    @FXML private TextField champTelephone;
    @FXML private TextField champEmail;
    @FXML private TextField champCarteFidelite;

    private Client clientSelectionne;

    @FXML
    public void initialize() {
        configurerColonnes();

        tableClients.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                chargerDansFormulaire(nouveau);
            }
        });

        rechercher();
    }

    /** Retour a l'ecran d'accueil (grille de tuiles), plein cadre. */
    @FXML
    private void retourMenu() {
        PrincipalController.getInstanceCourante().afficherMenuAccueil();
    }


    private void configurerColonnes() {
        colNomComplet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomComplet()));
        colTelephone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelephone()));
        colCarteFidelite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCarteFidelite()));
        colPoints.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPointsFidelite())));
    }

    @FXML
    private void rechercher() {
        String motCle = champRecherche.getText();
        List<Client> resultats = (motCle == null || motCle.isBlank())
                ? ContexteApplication.getInstance().getClientService().listerTous()
                : ContexteApplication.getInstance().getClientService().rechercher(motCle);
        tableClients.setItems(FXCollections.observableArrayList(resultats));
    }

    @FXML
    private void nouveauClient() {
        clientSelectionne = null;
        labelTitreFormulaire.setText("Nouveau client");
        champNom.clear();
        champPrenom.clear();
        champTelephone.clear();
        champEmail.clear();
        champCarteFidelite.clear();
        tableClients.getSelectionModel().clearSelection();
    }

    private void chargerDansFormulaire(Client c) {
        clientSelectionne = c;
        labelTitreFormulaire.setText("Modifier : " + c.getNomComplet());
        champNom.setText(c.getNom());
        champPrenom.setText(c.getPrenom());
        champTelephone.setText(c.getTelephone());
        champEmail.setText(c.getEmail());
        champCarteFidelite.setText(c.getCarteFidelite());
    }

    @FXML
    private void enregistrerClient() {
        try {
            Client c = (clientSelectionne != null) ? clientSelectionne : new Client();
            c.setNom(champNom.getText());
            c.setPrenom(champPrenom.getText());
            c.setTelephone(champTelephone.getText());
            c.setEmail(champEmail.getText());
            c.setCarteFidelite(champCarteFidelite.getText());

            if (clientSelectionne != null) {
                ContexteApplication.getInstance().getClientService().modifierClient(c);
            } else {
                ContexteApplication.getInstance().getClientService().creerClient(c);
            }
            rechercher();
            nouveauClient();
            DialogueUtil.afficherInfo("Client enregistre", "Les informations ont ete sauvegardees.");
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }
}
