package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.exception.AuthentificationException;
import com.supermarche.model.Utilisateur;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class ConnexionController {

    @FXML private TextField champNomUtilisateur;
    @FXML private PasswordField champMotDePasse;
    @FXML private Label labelErreur;
    @FXML private Button boutonConnexion;

    @FXML
    private void seConnecter() {
        String nomUtilisateur = champNomUtilisateur.getText();
        String motDePasse = champMotDePasse.getText();

        masquerErreur();

        if (nomUtilisateur == null || nomUtilisateur.isBlank() || motDePasse == null || motDePasse.isBlank()) {
            afficherErreur("Veuillez renseigner votre nom d'utilisateur et votre mot de passe.");
            return;
        }

        boutonConnexion.setDisable(true);
        try {
            Utilisateur utilisateur = ContexteApplication.getInstance().getAuthService()
                    .seConnecter(nomUtilisateur, motDePasse);
            champMotDePasse.clear();
            initialiserDepotParDefaut();
            com.supermarche.MainApp.afficherApplicationPrincipale(utilisateur);
        } catch (AuthentificationException e) {
            afficherErreur(e.getMessage());
            champMotDePasse.clear();
        } catch (IOException e) {
            afficherErreur("Erreur technique lors du chargement de l'application.");
        } catch (Exception e) {
            afficherErreur("Impossible de se connecter a la base de donnees. Verifiez que le serveur MariaDB est accessible.");
        } finally {
            boutonConnexion.setDisable(false);
        }
    }

    /**
     * Selectionne automatiquement le premier depot disponible comme depot
     * de travail par defaut pour ce poste. Avec un seul magasin (cas
     * courant), cela evite a l'utilisateur de devoir le choisir a chaque
     * connexion ; le choix de la caisse (module Caisse) reste manuel car
     * plusieurs caisses peuvent coexister dans le meme depot.
     */
    private void initialiserDepotParDefaut() {
        if (ContexteApplication.getInstance().getIdDepotCourant() == null) {
            new com.supermarche.dao.DepotDao().listerTous().stream()
                    .findFirst()
                    .ifPresent(depot -> ContexteApplication.getInstance().setIdDepotCourant(depot.getId()));
        }
    }

    private void afficherErreur(String message) {
        labelErreur.setText(message);
        labelErreur.setVisible(true);
        labelErreur.setManaged(true);
    }

    private void masquerErreur() {
        labelErreur.setVisible(false);
        labelErreur.setManaged(false);
    }
}
