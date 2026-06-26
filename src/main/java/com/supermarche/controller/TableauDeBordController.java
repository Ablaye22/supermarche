package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.Produit;
import com.supermarche.service.RapportService;
import com.supermarche.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TableauDeBordController {

    @FXML private Label labelCaJour;
    @FXML private Label labelNbVentesJour;
    @FXML private Label labelPanierMoyen;
    @FXML private Label labelValeurStock;

    @FXML private TableView<Produit> tableAlertesStock;
    @FXML private TableColumn<Produit, String> colAlerteDesignation;
    @FXML private TableColumn<Produit, String> colAlerteStock;
    @FXML private TableColumn<Produit, String> colAlerteSeuil;

    @FXML private TableView<RapportService.ProduitVendu> tableMeilleuresVentes;
    @FXML private TableColumn<RapportService.ProduitVendu, String> colTopDesignation;
    @FXML private TableColumn<RapportService.ProduitVendu, String> colTopQuantite;
    @FXML private TableColumn<RapportService.ProduitVendu, String> colTopCa;

    @FXML
    public void initialize() {
        configurerColonnes();
        chargerIndicateurs();
    }

    private void configurerColonnes() {
        colAlerteDesignation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDesignation()));
        Integer idDepot = ContexteApplication.getInstance().getIdDepotCourant();
        colAlerteStock.setCellValueFactory(c -> {
            int idd = idDepot != null ? idDepot : 1;
            int qte = ContexteApplication.getInstance().getStockService().lireQuantite(c.getValue().getId(), idd);
            return new SimpleStringProperty(String.valueOf(qte));
        });
        colAlerteSeuil.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getSeuilAlerteStock())));

        colTopDesignation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().designation()));
        colTopQuantite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().quantiteTotale().toPlainString()));
        colTopCa.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.montant(c.getValue().chiffreAffairesTtc())));
    }

    private void chargerIndicateurs() {
        RapportService rapportService = ContexteApplication.getInstance().getRapportService();

        LocalDateTime debutJour = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime finJour = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        RapportService.SyntheseVentes synthese = rapportService.syntheseVentes(debutJour, finJour);
        labelCaJour.setText(FormatUtil.montant(synthese.chiffreAffairesTtc()));
        labelNbVentesJour.setText(String.valueOf(synthese.nombreVentes()));
        labelPanierMoyen.setText(FormatUtil.montant(synthese.panierMoyen()));

        Integer idDepot = ContexteApplication.getInstance().getIdDepotCourant();
        int depotPourValeur = idDepot != null ? idDepot : 1;
        labelValeurStock.setText(FormatUtil.montant(rapportService.valeurStockDepot(depotPourValeur)));

        List<Produit> alertes = ContexteApplication.getInstance().getProduitService().listerEnAlerteStock(depotPourValeur);
        tableAlertesStock.setItems(FXCollections.observableArrayList(alertes));

        List<RapportService.ProduitVendu> topVentes = rapportService.meilleuresVentes(
                LocalDateTime.now().minusDays(30), LocalDateTime.now(), 10);
        tableMeilleuresVentes.setItems(FXCollections.observableArrayList(topVentes));
    }
}
