package com.supermarche.controller;

import com.supermarche.util.DialogueUtil;
import com.supermarche.config.ContexteApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Controleur racine de la fenetre principale.
 *
 * Navigation en "plein cadre" : soit le menu d'accueil (menu_accueil.fxml)
 * est affiche, soit une section (produits, stocks, ...) l'est -- jamais
 * les deux en meme temps, et aucune barre permanente ne les entoure.
 * Chaque section est responsable d'afficher son propre bouton "Retour"
 * qui rappelle {@link #afficherMenuAccueil()}.
 */
public class PrincipalController {

    /** Instance courante, exposee pour que les ecrans enfants (menu
     * d'accueil, sections) puissent demander une navigation sans avoir
     * besoin d'une reference explicite transmise a la construction. */
    private static PrincipalController instanceCourante;

    @FXML private VBox zoneContenu;

    @FXML
    public void initialize() {
        instanceCourante = this;
        afficherMenuAccueil();
    }

    public static PrincipalController getInstanceCourante() {
        return instanceCourante;
    }

    private void chargerVue(String cheminFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(cheminFxml));
            Parent vue = loader.load();
            zoneContenu.getChildren().setAll(vue);
            VBox.setVgrow(vue, Priority.ALWAYS);
        } catch (IOException e) {
            DialogueUtil.afficherErreur("Erreur", "Impossible de charger cette page : " + e.getMessage());
        }
    }

    /** Revient a l'ecran d'accueil (grille de tuiles). Appelee depuis le
     * bouton "Retour" de chaque section. */
    public void afficherMenuAccueil() {
        chargerVue("/fxml/menu_accueil.fxml");
    }

    public void ouvrirTableauDeBord() {
        chargerVue("/fxml/tableau_de_bord.fxml");
    }

    public void ouvrirActivitesJournalier() {
        chargerVue("/fxml/prelevement.fxml");
    }

    public void ouvrirProduits() {
        chargerVue("/fxml/produits.fxml");
    }

    public void ouvrirStocks() {
        chargerVue("/fxml/stocks.fxml");
    }

    public void ouvrirFournisseurs() {
        chargerVue("/fxml/fournisseurs.fxml");
    }

    public void ouvrirClients() {
        chargerVue("/fxml/clients.fxml");
    }

    public void ouvrirRapports() {
        chargerVue("/fxml/rapports.fxml");
    }

    public void ouvrirUtilisateurs() {
        chargerVue("/fxml/utilisateurs.fxml");
    }

    public void ouvrirPlanning() {
        chargerVue("/fxml/planning.fxml");
    }

    public void ouvrirJournal() {
        chargerVue("/fxml/journal_audit.fxml");
    }

    public void seDeconnecter() {
        boolean confirme = DialogueUtil.confirmer("Deconnexion", "Voulez-vous vraiment vous deconnecter ?");
        if (!confirme) {
            return;
        }
        ContexteApplication.getInstance().getAuthService().seDeconnecter();
        ContexteApplication.getInstance().reinitialiserSession();
        try {
            com.supermarche.MainApp.afficherEcranConnexion();
        } catch (IOException e) {
            DialogueUtil.afficherErreur("Erreur", "Impossible de revenir a l'ecran de connexion.");
        }
    }
}