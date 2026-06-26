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
    @FXML private ComboBox<Caisse> comboCaisse;
    @FXML private Label labelStatutSession;
    @FXML private TextField champFondOuverture;
    @FXML private Button boutonOuvrirSession;
    @FXML private Button boutonFermerSession;

    @FXML private TextField champRechercheProduit;
    @FXML private TableView<Produit> tableProduits;
    @FXML private TableColumn<Produit, String> colProduitDesignation;
    @FXML private TableColumn<Produit, String> colProduitPrix;
    @FXML private TableColumn<Produit, String> colProduitStock;

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

    @FXML
    public void initialize() {
        configurerColonnes();
        chargerCaissesDisponibles();
        comboModePaiement.setItems(FXCollections.observableArrayList(ModePaiement.values()));
        comboModePaiement.getSelectionModel().select(ModePaiement.ESPECES);
        tablePanier.setItems(panier);

        champRechercheProduit.setOnAction(e -> rechercherProduit());
        comboCaisse.valueProperty().addListener((obs, ancienne, nouvelle) -> verifierSessionPourCaisse());

        rafraichirEtatBoutonsSession();
    }

    private void configurerColonnes() {
        colProduitDesignation.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDesignation()));
        colProduitPrix.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(formaterMontant(c.getValue().getPrixVenteTtc())));
        colProduitStock.setCellValueFactory(c -> {
            int idDepot = ContexteApplication.getInstance().getIdDepotCourant() != null
                    ? ContexteApplication.getInstance().getIdDepotCourant() : -1;
            int quantite = idDepot > 0
                    ? ContexteApplication.getInstance().getStockService().lireQuantite(c.getValue().getId(), idDepot)
                    : 0;
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(quantite));
        });

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

    @FXML
    private void rechercherProduit() {
        String motCle = champRechercheProduit.getText();
        if (motCle == null || motCle.isBlank()) {
            return;
        }
        // Si le mot-cle correspond exactement a un code-barre, on l'ajoute directement
        // au panier (cas du flux normal : scan d'un produit a la caisse).
        Optional<Produit> parCodeBarre = ContexteApplication.getInstance().getProduitService().trouverParCodeBarre(motCle.trim());
        if (parCodeBarre.isPresent()) {
            ajouterProduitAuPanier(parCodeBarre.get());
            champRechercheProduit.clear();
            return;
        }
        List<Produit> resultats = ContexteApplication.getInstance().getProduitService().rechercher(motCle);
        tableProduits.setItems(FXCollections.observableArrayList(resultats));
    }

    @FXML
    private void ajouterAuPanier() {
        Produit selectionne = tableProduits.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            DialogueUtil.afficherAvertissement("Aucun produit selectionne", "Selectionnez un produit dans la liste.");
            return;
        }
        ajouterProduitAuPanier(selectionne);
    }

    private void ajouterProduitAuPanier(Produit produit) {
        if (sessionOuverte == null) {
            DialogueUtil.afficherAvertissement("Session requise", "Ouvrez une session de caisse avant d'enregistrer une vente.");
            return;
        }
        int idDepot = ContexteApplication.getInstance().getIdDepotCourant();
        int stockDisponible = ContexteApplication.getInstance().getStockService().lireQuantite(produit.getId(), idDepot);

        Optional<LignePanier> ligneExistante = panier.stream()
                .filter(l -> l.getProduit().getId().equals(produit.getId()))
                .findFirst();

        int quantiteDejaDansPanier = ligneExistante.map(LignePanier::getQuantite).orElse(0);
        if (quantiteDejaDansPanier + 1 > stockDisponible) {
            DialogueUtil.afficherAvertissement("Stock insuffisant",
                    "Stock disponible pour " + produit.getDesignation() + " : " + stockDisponible);
            return;
        }

        if (ligneExistante.isPresent()) {
            ligneExistante.get().setQuantite(ligneExistante.get().getQuantite() + 1);
            tablePanier.refresh();
        } else {
            panier.add(new LignePanier(produit, 1));
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
            rafraichirTotal();
        } catch (SupermarcheException e) {
            DialogueUtil.afficherErreur("Vente impossible", e.getMessage());
        }
    }

    private String formaterMontant(BigDecimal montant) {
        return com.supermarche.util.FormatUtil.montant(montant);
    }
}
