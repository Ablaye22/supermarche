package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.service.RapportService;
import com.supermarche.util.DialogueUtil;
import com.supermarche.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class RapportsController {

    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;

    @FXML private Label labelCaTtc;
    @FXML private Label labelCaHt;
    @FXML private Label labelTva;
    @FXML private Label labelNbVentes;

    @FXML private TableView<RapportService.VentilationParJour> tableVentesParJour;
    @FXML private TableColumn<RapportService.VentilationParJour, String> colJour;
    @FXML private TableColumn<RapportService.VentilationParJour, String> colTotalJour;
    @FXML private TableColumn<RapportService.VentilationParJour, String> colNbVentesJour;

    @FXML private TableView<RapportService.ProduitVendu> tableTopProduits;
    @FXML private TableColumn<RapportService.ProduitVendu, String> colProduit;
    @FXML private TableColumn<RapportService.ProduitVendu, String> colQuantite;
    @FXML private TableColumn<RapportService.ProduitVendu, String> colCaProduit;

    @FXML
    public void initialize() {
        configurerColonnes();
        dateDebut.setValue(LocalDate.now().minusDays(7));
        dateFin.setValue(LocalDate.now());
        genererRapport();
    }

    private void configurerColonnes() {
        colJour.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().jour().format(FormatUtil.FORMAT_DATE)));
        colTotalJour.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.montant(c.getValue().totalTtc())));
        colNbVentesJour.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().nombreVentes())));

        colProduit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().designation()));
        colQuantite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().quantiteTotale().toPlainString()));
        colCaProduit.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.montant(c.getValue().chiffreAffairesTtc())));
    }

    @FXML
    private void genererRapport() {
        LocalDate debut = dateDebut.getValue();
        LocalDate fin = dateFin.getValue();
        if (debut == null || fin == null || debut.isAfter(fin)) {
            DialogueUtil.afficherAvertissement("Periode invalide", "Verifiez que la date de debut precede la date de fin.");
            return;
        }

        LocalDateTime debutDateTime = LocalDateTime.of(debut, LocalTime.MIN);
        LocalDateTime finDateTime = LocalDateTime.of(fin, LocalTime.MAX);

        RapportService rapportService = ContexteApplication.getInstance().getRapportService();

        RapportService.SyntheseVentes synthese = rapportService.syntheseVentes(debutDateTime, finDateTime);
        labelCaTtc.setText(FormatUtil.montant(synthese.chiffreAffairesTtc()));
        labelCaHt.setText(FormatUtil.montant(synthese.chiffreAffairesHt()));
        labelTva.setText(FormatUtil.montant(synthese.totalTva()));
        labelNbVentes.setText(String.valueOf(synthese.nombreVentes()));

        List<RapportService.VentilationParJour> parJour = rapportService.ventesParJour(debutDateTime, finDateTime);
        tableVentesParJour.setItems(FXCollections.observableArrayList(parJour));

        List<RapportService.ProduitVendu> topProduits = rapportService.meilleuresVentes(debutDateTime, finDateTime, 20);
        tableTopProduits.setItems(FXCollections.observableArrayList(topProduits));
    }
}
