package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.exception.SupermarcheException;
import com.supermarche.model.Caisse;
import com.supermarche.model.Client;
import com.supermarche.model.LigneVente;
import com.supermarche.model.ModePaiement;
import com.supermarche.model.Paiement;
import com.supermarche.model.Produit;
import com.supermarche.model.SessionCaisse;
import com.supermarche.model.Vente;
import com.supermarche.util.DialogueUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CaisseController {

    @FXML private VBox panneauSession;
    @FXML private Button boutonReduireMenu;
    @FXML private ComboBox<Caisse> comboCaisse;
    @FXML private Label labelStatutSession;
    @FXML private TextField champFondOuverture;
    @FXML private Button boutonOuvrirSession;
    @FXML private Button boutonFermerSession;

    @FXML private Label labelAfficheurPave;
    @FXML private Label labelDernierProduitAjoute;

    @FXML private TableView<LignePanier> tablePanier;
    @FXML private TableColumn<LignePanier, String> colPanierDesignation;
    @FXML private TableColumn<LignePanier, Number> colPanierQuantite;
    @FXML private TableColumn<LignePanier, String> colPanierPrixUnitaire;
    @FXML private TableColumn<LignePanier, String> colPanierTotal;

    @FXML private TextField champCarteFidelite;
    @FXML private Label labelTotalTicket;
    @FXML private ComboBox<ModePaiement> comboModePaiement;
    @FXML private TextField champMontantPaiement;

    private final ObservableList<LignePanier> panier = FXCollections.observableArrayList();
    private SessionCaisse sessionOuverte;

    /** Saisie en cours sur le pave numerique, sous forme de texte brut (chiffres uniquement). */
    private final StringBuilder saisieCourante = new StringBuilder();

    @FXML
    public void initialize() {
        configurerColonnes();
        chargerCaissesDisponibles();
        comboModePaiement.setItems(FXCollections.observableArrayList(ModePaiement.values()));
        comboModePaiement.getSelectionModel().select(ModePaiement.ESPECES);
        tablePanier.setItems(panier);

        comboCaisse.valueProperty().addListener((obs, ancienne, nouvelle) -> verifierSessionPourCaisse());

        rafraichirEtatBoutonsSession();
        rafraichirAfficheurPave();

        // En arrivant sur l'ecran Caisse, on s'assure que la barre laterale
        // demarre en mode normal (elle a pu rester reduite si on revient
        // sur cet ecran apres l'avoir quitte en mode reduit).
        if (PrincipalController.getInstanceCourante() != null) {
            PrincipalController.getInstanceCourante().definirBarreReduite(false);
        }
    }

    private void configurerColonnes() {
        colPanierDesignation.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDesignation()));
        colPanierQuantite.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getQuantite()));
        colPanierPrixUnitaire.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(formaterMontant(c.getValue().getPrixUnitaire())));
        colPanierTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(formaterMontant(c.getValue().getTotalLigne())));
    }

    private void chargerCaissesDisponibles() {
        List<Caisse> caisses = ContexteApplication.getInstance().getCaisseService().listerCaissesActives();
        comboCaisse.setItems(FXCollections.observableArrayList(caisses));
        comboCaisse.setConverter(new StringConverter<>() {
            @Override
            public String toString(Caisse caisse) {
                return caisse == null ? "" : caisse.getNom();
            }

            @Override
            public Caisse fromString(String s) {
                return null;
            }
        });
        if (!caisses.isEmpty()) {
            comboCaisse.getSelectionModel().selectFirst();
        }
    }

    private void verifierSessionPourCaisse() {
        Caisse caisse = comboCaisse.getValue();
        if (caisse == null) {
            return;
        }
        Optional<SessionCaisse> session = ContexteApplication.getInstance().getCaisseService()
                .trouverSessionOuverte(caisse.getId());
        if (session.isPresent()) {
            this.sessionOuverte = session.get();
            ContexteApplication.getInstance().setIdSessionCaisseCourante(sessionOuverte.getId());
            ContexteApplication.getInstance().setIdCaisseCourante(caisse.getId());
            ContexteApplication.getInstance().setIdDepotCourant(caisse.getIdDepot());
        } else {
            this.sessionOuverte = null;
        }
        rafraichirEtatBoutonsSession();
    }

    private void rafraichirEtatBoutonsSession() {
        boolean sessionActive = sessionOuverte != null;
        labelStatutSession.setText(sessionActive
                ? "Session ouverte par " + sessionOuverte.getNomUtilisateur()
                : "Aucune session ouverte");
        labelStatutSession.getStyleClass().removeAll("badge-succes", "badge-neutre");
        labelStatutSession.getStyleClass().add(sessionActive ? "badge-succes" : "badge-neutre");

        boutonOuvrirSession.setDisable(sessionActive);
        boutonFermerSession.setDisable(!sessionActive);
        champFondOuverture.setDisable(sessionActive);
    }

    @FXML
    private void ouvrirSession() {
        Caisse caisse = comboCaisse.getValue();
        if (caisse == null) {
            DialogueUtil.afficherAvertissement("Caisse requise", "Selectionnez une caisse avant d'ouvrir une session.");
            return;
        }
        BigDecimal fond;
        try {
            fond = new BigDecimal(champFondOuverture.getText().replace(",", "."));
        } catch (Exception e) {
            DialogueUtil.afficherAvertissement("Montant invalide", "Saisissez un montant numerique pour le fond de caisse.");
            return;
        }

        try {
            sessionOuverte = ContexteApplication.getInstance().getCaisseService().ouvrirSession(caisse.getId(), fond);
            ContexteApplication.getInstance().setIdSessionCaisseCourante(sessionOuverte.getId());
            ContexteApplication.getInstance().setIdCaisseCourante(caisse.getId());
            ContexteApplication.getInstance().setIdDepotCourant(caisse.getIdDepot());
            rafraichirEtatBoutonsSession();
        } catch (SupermarcheException e) {
            DialogueUtil.afficherErreur("Ouverture impossible", e.getMessage());
        }
    }

    @FXML
    private void fermerSession() {
        if (sessionOuverte == null) {
            return;
        }
        Optional<String> saisie = DialogueUtil.demanderSaisie("Fermeture de caisse",
                "Montant reel compte en caisse (€) :", "");
        if (saisie.isEmpty()) {
            return;
        }
        try {
            BigDecimal fondReel = new BigDecimal(saisie.get().replace(",", "."));
            SessionCaisse fermee = ContexteApplication.getInstance().getCaisseService()
                    .fermerSession(sessionOuverte.getId(), fondReel);
            String messageEcart = fermee.getEcart().compareTo(BigDecimal.ZERO) == 0
                    ? "Aucun ecart constate."
                    : "Ecart constate : " + formaterMontant(fermee.getEcart())
                    + (fermee.getEcart().compareTo(BigDecimal.ZERO) > 0 ? " (excedent)" : " (manquant)");
            DialogueUtil.afficherInfo("Session fermee", "Fond theorique : " + formaterMontant(fermee.getFondFermetureTheorique())
                    + "\nFond reel : " + formaterMontant(fondReel) + "\n" + messageEcart);
            sessionOuverte = null;
            ContexteApplication.getInstance().reinitialiserSession();
            rafraichirEtatBoutonsSession();
        } catch (NumberFormatException e) {
            DialogueUtil.afficherAvertissement("Montant invalide", "Saisissez un montant numerique.");
        } catch (SupermarcheException e) {
            DialogueUtil.afficherErreur("Fermeture impossible", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Menu lateral reductible (mode icones) propre a l'ecran Caisse
    // ------------------------------------------------------------------

    @FXML
    private void basculerMenuReduit() {
        PrincipalController principal = PrincipalController.getInstanceCourante();
        if (principal == null) {
            return;
        }
        principal.definirBarreReduite(!principal.isBarreReduite());
    }

    // ------------------------------------------------------------------
    // Pave numerique : saisie de code-barres puis, optionnellement, de
    // quantite a appliquer sur la derniere ligne ajoutee au panier.
    // ------------------------------------------------------------------

    @FXML
    private void saisirChiffre(javafx.event.ActionEvent evenement) {
        Button bouton = (Button) evenement.getSource();
        // Limite raisonnable pour un code-barres EAN-13 ou une quantite : evite une saisie demesuree par erreur.
        if (saisieCourante.length() >= 13) {
            return;
        }
        saisieCourante.append(bouton.getText());
        rafraichirAfficheurPave();
    }

    @FXML
    private void effacerSaisie() {
        saisieCourante.setLength(0);
        rafraichirAfficheurPave();
    }

    @FXML
    private void effacerDernierChiffre() {
        if (saisieCourante.length() > 0) {
            saisieCourante.deleteCharAt(saisieCourante.length() - 1);
        }
        rafraichirAfficheurPave();
    }

    private void rafraichirAfficheurPave() {
        labelAfficheurPave.setText(saisieCourante.length() == 0 ? "0" : saisieCourante.toString());
    }

    /** Bouton "Valider code-barres" : recherche le produit et l'ajoute au panier avec une quantite de 1. */
    @FXML
    private void validerCodeBarrePave() {
        String codeBarre = saisieCourante.toString();
        if (codeBarre.isBlank()) {
            DialogueUtil.afficherAvertissement("Saisie vide", "Saisissez un code-barres avant de valider.");
            return;
        }
        if (sessionOuverte == null) {
            DialogueUtil.afficherAvertissement("Session requise", "Ouvrez une session de caisse avant d'enregistrer une vente.");
            return;
        }

        Optional<Produit> produit = ContexteApplication.getInstance().getProduitService().trouverParCodeBarre(codeBarre);
        if (produit.isEmpty()) {
            DialogueUtil.afficherAvertissement("Produit introuvable", "Aucun produit ne correspond au code-barres " + codeBarre + ".");
            effacerSaisie();
            return;
        }

        ajouterProduitAuPanier(produit.get(), 1);
        labelDernierProduitAjoute.setText("Dernier article : " + produit.get().getDesignation());
        effacerSaisie();
    }

    /**
     * Bouton "Appliquer comme quantite" : applique le nombre saisi sur le
     * pave comme nouvelle quantite de la derniere ligne ajoutee au panier
     * (flux classique de caisse : on scanne le produit, puis si besoin on
     * tape la quantite reelle, ex. plusieurs unites du meme article).
     */
    @FXML
    private void appliquerQuantitePave() {
        String saisie = saisieCourante.toString();
        if (saisie.isBlank()) {
            DialogueUtil.afficherAvertissement("Saisie vide", "Saisissez une quantite avant de l'appliquer.");
            return;
        }
        if (panier.isEmpty()) {
            DialogueUtil.afficherAvertissement("Panier vide", "Ajoutez d'abord un produit via son code-barres.");
            effacerSaisie();
            return;
        }

        int quantiteSaisie;
        try {
            quantiteSaisie = Integer.parseInt(saisie);
        } catch (NumberFormatException e) {
            DialogueUtil.afficherAvertissement("Quantite invalide", "Saisissez un nombre entier.");
            return;
        }
        if (quantiteSaisie <= 0) {
            DialogueUtil.afficherAvertissement("Quantite invalide", "La quantite doit etre superieure a zero.");
            return;
        }

        LignePanier derniereLigne = panier.get(panier.size() - 1);
        int idDepot = ContexteApplication.getInstance().getIdDepotCourant();
        int stockDisponible = ContexteApplication.getInstance().getStockService()
                .lireQuantite(derniereLigne.getProduit().getId(), idDepot);

        if (quantiteSaisie > stockDisponible) {
            DialogueUtil.afficherAvertissement("Stock insuffisant",
                    "Stock disponible pour " + derniereLigne.getDesignation() + " : " + stockDisponible);
            return;
        }

        derniereLigne.setQuantite(quantiteSaisie);
        tablePanier.refresh();
        rafraichirTotal();
        effacerSaisie();
    }

    private void ajouterProduitAuPanier(Produit produit, int quantiteAjoutee) {
        int idDepot = ContexteApplication.getInstance().getIdDepotCourant();
        int stockDisponible = ContexteApplication.getInstance().getStockService().lireQuantite(produit.getId(), idDepot);

        Optional<LignePanier> ligneExistante = panier.stream()
                .filter(l -> l.getProduit().getId().equals(produit.getId()))
                .findFirst();

        int quantiteDejaDansPanier = ligneExistante.map(LignePanier::getQuantite).orElse(0);
        if (quantiteDejaDansPanier + quantiteAjoutee > stockDisponible) {
            DialogueUtil.afficherAvertissement("Stock insuffisant",
                    "Stock disponible pour " + produit.getDesignation() + " : " + stockDisponible);
            return;
        }

        if (ligneExistante.isPresent()) {
            ligneExistante.get().setQuantite(ligneExistante.get().getQuantite() + quantiteAjoutee);
            tablePanier.refresh();
        } else {
            panier.add(new LignePanier(produit, quantiteAjoutee));
        }
        rafraichirTotal();
    }

    @FXML
    private void retirerLigneSelectionnee() {
        LignePanier selectionnee = tablePanier.getSelectionModel().getSelectedItem();
        if (selectionnee != null) {
            panier.remove(selectionnee);
            rafraichirTotal();
        }
    }

    @FXML
    private void viderPanier() {
        if (!panier.isEmpty() && !DialogueUtil.confirmer("Annuler le ticket", "Voulez-vous vider le panier en cours ?")) {
            return;
        }
        panier.clear();
        champCarteFidelite.clear();
        labelDernierProduitAjoute.setText("Dernier article : (aucun)");
        rafraichirTotal();
    }

    private void rafraichirTotal() {
        BigDecimal total = panier.stream().map(LignePanier::getTotalLigne).reduce(BigDecimal.ZERO, BigDecimal::add);
        labelTotalTicket.setText(formaterMontant(total));
    }

    @FXML
    private void validerVente() {
        if (sessionOuverte == null) {
            DialogueUtil.afficherAvertissement("Session requise", "Ouvrez une session de caisse avant d'enregistrer une vente.");
            return;
        }
        if (panier.isEmpty()) {
            DialogueUtil.afficherAvertissement("Panier vide", "Ajoutez au moins un produit avant de valider.");
            return;
        }

        BigDecimal montantPaye;
        try {
            String saisie = champMontantPaiement.getText();
            BigDecimal total = panier.stream().map(LignePanier::getTotalLigne).reduce(BigDecimal.ZERO, BigDecimal::add);
            montantPaye = (saisie == null || saisie.isBlank()) ? total : new BigDecimal(saisie.replace(",", "."));
        } catch (NumberFormatException e) {
            DialogueUtil.afficherAvertissement("Montant invalide", "Saisissez un montant de paiement numerique.");
            return;
        }

        Integer idClient = null;
        String carte = champCarteFidelite.getText();
        if (carte != null && !carte.isBlank()) {
            Optional<Client> client = ContexteApplication.getInstance().getClientService().trouverParCarteFidelite(carte.trim());
            if (client.isPresent()) {
                idClient = client.get().getId();
            } else {
                DialogueUtil.afficherAvertissement("Carte inconnue",
                        "Aucun client ne correspond a cette carte de fidelite. La vente continuera sans client associe.");
            }
        }

        List<LigneVente> lignes = new ArrayList<>();
        for (LignePanier lp : panier) {
            Produit p = lp.getProduit();
            LigneVente ligne = new LigneVente(p.getId(), p.getDesignation(),
                    BigDecimal.valueOf(lp.getQuantite()), p.getPrixVenteTtc(), p.getTauxTva());
            lignes.add(ligne);
        }

        List<Paiement> paiements = List.of(new Paiement(comboModePaiement.getValue(), montantPaye));

        try {
            Vente vente = ContexteApplication.getInstance().getVenteService().enregistrerVente(
                    sessionOuverte.getId(), ContexteApplication.getInstance().getIdDepotCourant(),
                    idClient, lignes, paiements);

            BigDecimal monnaieARendre = montantPaye.subtract(vente.getTotalTtc());
            String messageMonnaie = monnaieARendre.compareTo(BigDecimal.ZERO) > 0
                    ? "\nMonnaie a rendre : " + formaterMontant(monnaieARendre) : "";
            DialogueUtil.afficherInfo("Vente enregistree",
                    "Ticket " + vente.getNumeroTicket() + "\nTotal : " + formaterMontant(vente.getTotalTtc()) + messageMonnaie);

            panier.clear();
            champCarteFidelite.clear();
            champMontantPaiement.clear();
            labelDernierProduitAjoute.setText("Dernier article : (aucun)");
            rafraichirTotal();
        } catch (SupermarcheException e) {
            DialogueUtil.afficherErreur("Vente impossible", e.getMessage());
        }
    }

    private String formaterMontant(BigDecimal montant) {
        return com.supermarche.util.FormatUtil.montant(montant);
    }
}