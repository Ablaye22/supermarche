package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.MouvementStock;
// import com.supermarche.model.Produit;
import com.supermarche.model.Stock;
import com.supermarche.util.DialogueUtil;
import com.supermarche.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
// import java.util.Optional;

public class StocksController {

    @FXML private TableView<Stock> tableStocks;
    @FXML private TableColumn<Stock, String> colStockDesignation;
    @FXML private TableColumn<Stock, String> colStockQuantite;
    @FXML private TableColumn<Stock, String> colStockStatut;

    @FXML private Label labelProduitSelectionne;
    @FXML private Label labelQuantiteActuelle;
    @FXML private TextField champQuantiteEntree;
    @FXML private TextField champNouvelleQuantite;
    @FXML private TextArea champCommentaireAjustement;

    @FXML private TableView<MouvementStock> tableHistorique;
    @FXML private TableColumn<MouvementStock, String> colHistoDate;
    @FXML private TableColumn<MouvementStock, String> colHistoType;
    @FXML private TableColumn<MouvementStock, String> colHistoQuantite;

    private Stock stockSelectionne;
    private int idDepotCourant;

    @FXML
    public void initialize() {
        idDepotCourant = ContexteApplication.getInstance().getIdDepotCourant() != null
                ? ContexteApplication.getInstance().getIdDepotCourant() : 1;

        configurerColonnes();

        tableStocks.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                selectionnerStock(nouveau);
            }
        });

        chargerStocks();
    }


    /** Retour a l'ecran d'accueil (grille de tuiles), plein cadre. */
    @FXML
    private void retourMenu() {
        PrincipalController.getInstanceCourante().afficherMenuAccueil();
    }


    private void configurerColonnes() {
        colStockDesignation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDesignationProduit()));
        colStockQuantite.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantite())));
        colStockStatut.setCellValueFactory(c -> {
            int qte = c.getValue().getQuantite();
            String statut = qte == 0 ? "Rupture" : (qte <= 5 ? "Stock bas" : "OK");
            return new SimpleStringProperty(statut);
        });

        colHistoDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getHorodatage() != null ? c.getValue().getHorodatage().format(FormatUtil.FORMAT_DATE_HEURE) : ""));
        colHistoType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeMouvement().name()));
        colHistoQuantite.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantite())));
    }

    private void chargerStocks() {
        List<Stock> stocks = ContexteApplication.getInstance().getStockService().listerParDepot(idDepotCourant);
        tableStocks.setItems(FXCollections.observableArrayList(stocks));
    }

    private void selectionnerStock(Stock stock) {
        this.stockSelectionne = stock;
        labelProduitSelectionne.setText(stock.getDesignationProduit());
        labelQuantiteActuelle.setText("Quantite actuelle : " + stock.getQuantite());
        champQuantiteEntree.clear();
        champNouvelleQuantite.clear();
        champCommentaireAjustement.clear();

        List<MouvementStock> historique = ContexteApplication.getInstance().getStockService()
                .historiqueParProduit(stock.getIdProduit());
        tableHistorique.setItems(FXCollections.observableArrayList(historique));
    }

    @FXML
    private void enregistrerEntree() {
        if (stockSelectionne == null) {
            DialogueUtil.afficherAvertissement("Aucun produit selectionne", "Selectionnez un produit dans la liste.");
            return;
        }
        try {
            int quantite = Integer.parseInt(champQuantiteEntree.getText());
            ContexteApplication.getInstance().getStockService().enregistrerEntree(
                    stockSelectionne.getIdProduit(), idDepotCourant, quantite, "Entree manuelle");
            chargerStocks();
            DialogueUtil.afficherInfo("Stock mis a jour", "Entree de " + quantite + " unite(s) enregistree.");
        } catch (NumberFormatException e) {
            DialogueUtil.afficherAvertissement("Quantite invalide", "Saisissez un nombre entier positif.");
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }

    @FXML
    private void ajusterStock() {
        if (stockSelectionne == null) {
            DialogueUtil.afficherAvertissement("Aucun produit selectionne", "Selectionnez un produit dans la liste.");
            return;
        }
        try {
            int nouvelleQuantite = Integer.parseInt(champNouvelleQuantite.getText());
            String commentaire = champCommentaireAjustement.getText();
            if (commentaire == null || commentaire.isBlank()) {
                DialogueUtil.afficherAvertissement("Motif requis", "Indiquez un motif pour cet ajustement (tracabilite).");
                return;
            }
            if (!DialogueUtil.confirmer("Confirmer l'ajustement",
                    "Le stock passera de " + stockSelectionne.getQuantite() + " a " + nouvelleQuantite + ". Confirmer ?")) {
                return;
            }
            ContexteApplication.getInstance().getStockService().ajusterStock(
                    stockSelectionne.getIdProduit(), idDepotCourant, nouvelleQuantite, commentaire);
            chargerStocks();
            DialogueUtil.afficherInfo("Stock ajuste", "Le stock a ete mis a jour.");
        } catch (NumberFormatException e) {
            DialogueUtil.afficherAvertissement("Quantite invalide", "Saisissez un nombre entier.");
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }
}
