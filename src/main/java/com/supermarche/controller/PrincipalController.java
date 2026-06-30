package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.Utilisateur;
import com.supermarche.util.DialogueUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class PrincipalController {

    /** Instance courante, exposee pour permettre a un ecran enfant (ex: Caisse) de reduire/etendre la barre laterale. */
    private static PrincipalController instanceCourante;

    @FXML private VBox barreLaterale;
    @FXML private Label labelTitreApp;
    @FXML private Label labelUtilisateurConnecte;
    @FXML private VBox zoneContenu;

    @FXML private Button boutonTableauDeBord;
    @FXML private Button boutonCaisse;
    @FXML private Button boutonProduits;
    @FXML private Button boutonStocks;
    @FXML private Button boutonFournisseurs;
    @FXML private Button boutonClients;
    @FXML private Button boutonRapports;
    @FXML private Button boutonUtilisateurs;
    @FXML private Button boutonJournal;
    @FXML private Button boutonDeconnexion;

    private Button boutonActif;
    private boolean barreReduite = false;

    /** Libelles complets des boutons, memorises pour pouvoir les restaurer apres une reduction en icones. */
    private final java.util.Map<Button, String> libellesComplets = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        instanceCourante = this;

        Utilisateur utilisateur = ContexteApplication.getInstance().getAuthService().getUtilisateurConnecte();
        if (utilisateur != null) {
            labelUtilisateurConnecte.setText(
                    utilisateur.getEmploye().getNomComplet() + " — " + utilisateur.getRole().getLibelle());
        }

        memoriserLibelles();
        appliquerVisibiliteSelonPermissions(utilisateur);
        ouvrirTableauDeBord();
    }

    private void memoriserLibelles() {
        for (Button b : new Button[]{boutonTableauDeBord, boutonCaisse, boutonProduits, boutonStocks,
                boutonFournisseurs, boutonClients, boutonRapports, boutonUtilisateurs, boutonJournal, boutonDeconnexion}) {
            libellesComplets.put(b, b.getText());
        }
    }

    /**
     * Reduit la barre laterale a une mini-barre d'icones (boutons sans
     * libelle, largeur fixe reduite) ou la restaure a sa largeur normale
     * avec libelles. Utilise notamment par l'ecran Caisse, qui a besoin
     * de davantage d'espace horizontal pour le pave numerique.
     */
    public void definirBarreReduite(boolean reduite) {
        if (this.barreReduite == reduite) {
            return;
        }
        this.barreReduite = reduite;

        barreLaterale.setPrefWidth(reduite ? 64 : 220);
        labelTitreApp.setVisible(!reduite);
        labelTitreApp.setManaged(!reduite);
        labelUtilisateurConnecte.setVisible(!reduite);
        labelUtilisateurConnecte.setManaged(!reduite);

        for (java.util.Map.Entry<Button, String> entree : libellesComplets.entrySet()) {
            Button bouton = entree.getKey();
            bouton.setText(reduite ? "" : entree.getValue());
        }
    }

    public boolean isBarreReduite() {
        return barreReduite;
    }

    /**
     * Acces a l'instance courante du controleur principal, pour les
     * controleurs d'ecrans enfants (charges dans zoneContenu) qui ont
     * besoin d'agir sur la navigation globale, comme reduire la barre
     * laterale. Simple et suffisant ici car une seule fenetre principale
     * existe a la fois dans cette application desktop.
     */
    public static PrincipalController getInstanceCourante() {
        return instanceCourante;
    }

    /**
     * Cache les boutons de navigation vers des modules pour lesquels
     * l'utilisateur n'a aucune permission utile. Ce filtrage cote IHM est
     * un confort d'usage (eviter de montrer des portes fermees) ; la
     * veritable barriere de securite reste le controle de permission
     * fait par chaque service avant toute operation sensible.
     */
    private void appliquerVisibiliteSelonPermissions(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return;
        }
        boutonProduits.setVisible(utilisateur.possedePermission("PRODUIT_GERER"));
        boutonProduits.setManaged(utilisateur.possedePermission("PRODUIT_GERER"));

        boutonStocks.setVisible(utilisateur.possedePermission("STOCK_GERER"));
        boutonStocks.setManaged(utilisateur.possedePermission("STOCK_GERER"));

        boutonFournisseurs.setVisible(utilisateur.possedePermission("FOURNISSEUR_GERER"));
        boutonFournisseurs.setManaged(utilisateur.possedePermission("FOURNISSEUR_GERER"));

        boutonCaisse.setVisible(utilisateur.possedePermission("VENTE_CREER"));
        boutonCaisse.setManaged(utilisateur.possedePermission("VENTE_CREER"));

        boutonRapports.setVisible(utilisateur.possedePermission("RAPPORT_VOIR"));
        boutonRapports.setManaged(utilisateur.possedePermission("RAPPORT_VOIR"));

        boolean estAdmin = utilisateur.possedePermission("UTILISATEUR_GERER");
        boutonUtilisateurs.setVisible(estAdmin);
        boutonUtilisateurs.setManaged(estAdmin);
        boutonJournal.setVisible(utilisateur.possedePermission("JOURNAL_VOIR"));
        boutonJournal.setManaged(utilisateur.possedePermission("JOURNAL_VOIR"));
    }

    private void chargerVue(String cheminFxml, Button boutonCorrespondant) {
        try {
            // En quittant l'ecran Caisse vers une autre page, la barre laterale
            // est toujours restauree a sa taille normale (la reduction en icones
            // n'a de sens que pendant la saisie en caisse).
            if (barreReduite) {
                definirBarreReduite(false);
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(cheminFxml));
            Parent vue = loader.load();
            zoneContenu.getChildren().setAll(vue);
            VBox.setVgrow(vue, javafx.scene.layout.Priority.ALWAYS);
            marquerBoutonActif(boutonCorrespondant);
        } catch (IOException e) {
            DialogueUtil.afficherErreur("Erreur", "Impossible de charger cette page : " + e.getMessage());
        }
    }

    private void marquerBoutonActif(Button bouton) {
        if (boutonActif != null) {
            boutonActif.getStyleClass().remove("bouton-nav-actif");
        }
        if (bouton != null) {
            bouton.getStyleClass().add("bouton-nav-actif");
            boutonActif = bouton;
        }
    }

    @FXML
    private void ouvrirTableauDeBord() {
        chargerVue("/fxml/tableau_de_bord.fxml", boutonTableauDeBord);
    }

    @FXML
    private void ouvrirCaisse() {
        chargerVue("/fxml/caisse.fxml", boutonCaisse);
    }

    @FXML
    private void ouvrirProduits() {
        chargerVue("/fxml/produits.fxml", boutonProduits);
    }

    @FXML
    private void ouvrirStocks() {
        chargerVue("/fxml/stocks.fxml", boutonStocks);
    }

    @FXML
    private void ouvrirFournisseurs() {
        chargerVue("/fxml/fournisseurs.fxml", boutonFournisseurs);
    }

    @FXML
    private void ouvrirClients() {
        chargerVue("/fxml/clients.fxml", boutonClients);
    }

    @FXML
    private void ouvrirRapports() {
        chargerVue("/fxml/rapports.fxml", boutonRapports);
    }

    @FXML
    private void ouvrirUtilisateurs() {
        chargerVue("/fxml/utilisateurs.fxml", boutonUtilisateurs);
    }

    @FXML
    private void ouvrirJournal() {
        chargerVue("/fxml/journal_audit.fxml", boutonJournal);
    }

    @FXML
    private void seDeconnecter() {
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