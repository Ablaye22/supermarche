package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.Utilisateur;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

/**
 * Ecran d'accueil : une grille de tuiles (icone + libelle centre) qui
 * remplace l'ancienne barre laterale permanente. Chaque tuile ouvre sa
 * section en plein cadre via PrincipalController ; chaque section
 * ramene ici via son propre bouton "Retour".
 */
public class MenuAccueilController {

    @FXML private Label labelUtilisateurConnecte;

    @FXML private Button tuileVente;
    @FXML private Button tuileProduits;
    @FXML private Button tuileStocks;
    @FXML private Button tuileFournisseurs;
    @FXML private Button tuileClients;
    @FXML private Button tuileRapports;
    @FXML private Button tuileUtilisateurs;
    @FXML private Button tuilePlanning;
    @FXML private Button tuileJournal;
    @FXML private Button tuileActivitesJournalier;

    @FXML
    public void initialize() {
        Utilisateur utilisateur = ContexteApplication.getInstance().getAuthService().getUtilisateurConnecte();
        if (utilisateur != null) {
            labelUtilisateurConnecte.setText(
                    utilisateur.getEmploye().getNomComplet() + " — " + utilisateur.getRole().getLibelle());
        }

        chargerIcone(tuileVente, "ventes.png");
        chargerIcone(tuileProduits, "produits.png");
        chargerIcone(tuileStocks, "stocks.png");
        chargerIcone(tuileFournisseurs, "fournisseurs.png");
        chargerIcone(tuileClients, "clients.png");
        chargerIcone(tuileRapports, "rapports.png");
        chargerIcone(tuileUtilisateurs, "utilisateurs.png");
        chargerIcone(tuilePlanning, "planning.png");
        chargerIcone(tuileJournal, "journal.png");
        chargerIcone(tuileActivitesJournalier, "prelevement.png");

        appliquerVisibiliteSelonPermissions(utilisateur);
    }

    private void chargerIcone(Button bouton, String nomFichier) {
        Image image = new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/images/slogan/" + nomFichier)));
        ImageView iv = new ImageView(image);
        iv.setFitWidth(50);
        iv.setFitHeight(50);
        iv.setPreserveRatio(true);
        bouton.setGraphic(iv);
    }

    /**
     * Meme logique de filtrage que l'ancienne barre laterale : cacher
     * les tuiles vers des modules sans permission utile. Reste un
     * confort d'usage ; la veritable barriere reste cote service.
     */
    private void appliquerVisibiliteSelonPermissions(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return;
        }
        definirVisible(tuileProduits, utilisateur.possedePermission("PRODUIT_GERER"));
        definirVisible(tuileStocks, utilisateur.possedePermission("STOCK_GERER"));
        definirVisible(tuileFournisseurs, utilisateur.possedePermission("FOURNISSEUR_GERER"));
        definirVisible(tuileRapports, utilisateur.possedePermission("RAPPORT_VOIR"));
        definirVisible(tuileUtilisateurs, utilisateur.possedePermission("UTILISATEUR_GERER"));
        definirVisible(tuileJournal, utilisateur.possedePermission("JOURNAL_VOIR"));
    }

    private void definirVisible(Button bouton, boolean visible) {
        bouton.setVisible(visible);
        bouton.setManaged(visible);
    }

    @FXML private void ouvrirVente() { PrincipalController.getInstanceCourante().ouvrirTableauDeBord(); }
    @FXML private void ouvrirProduits() { PrincipalController.getInstanceCourante().ouvrirProduits(); }
    @FXML private void ouvrirStocks() { PrincipalController.getInstanceCourante().ouvrirStocks(); }
    @FXML private void ouvrirFournisseurs() { PrincipalController.getInstanceCourante().ouvrirFournisseurs(); }
    @FXML private void ouvrirClients() { PrincipalController.getInstanceCourante().ouvrirClients(); }
    @FXML private void ouvrirRapports() { PrincipalController.getInstanceCourante().ouvrirRapports(); }
    @FXML private void ouvrirUtilisateurs() { PrincipalController.getInstanceCourante().ouvrirUtilisateurs(); }
    @FXML private void ouvrirPlanning() { PrincipalController.getInstanceCourante().ouvrirPlanning(); }
    @FXML private void ouvrirJournal() { PrincipalController.getInstanceCourante().ouvrirJournal(); }
    @FXML private void ouvrirActivitesJournalier() { PrincipalController.getInstanceCourante().ouvrirActivitesJournalier(); }

    @FXML
    private void seDeconnecter() {
        PrincipalController.getInstanceCourante().seDeconnecter();
    }
}
