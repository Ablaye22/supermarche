package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.service.RapportService;
import com.supermarche.util.DialogueUtil;
import com.supermarche.util.FormatUtil;
import com.supermarche.util.RapportPdfExporter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    /** Dernier resultat calcule, conserve pour permettre l'export PDF
     * sans refaire la requete. */
    private RapportService.SyntheseVentes derniereSynthese;
    private List<RapportService.VentilationParJour> dernierParJour;
    private List<RapportService.ProduitVendu> dernierTopProduits;
    private LocalDate derniereDateDebut;
    private LocalDate derniereDateFin;

    @FXML
    public void initialize() {
        configurerColonnes();
        dateDebut.setValue(LocalDate.now().minusDays(7));
        dateFin.setValue(LocalDate.now());
        // genererRapport();
    }

    /** Retour a l'ecran d'accueil (grille de tuiles), plein cadre. */
    @FXML
    private void retourMenu() {
        PrincipalController.getInstanceCourante().afficherMenuAccueil();
    }

    private void configurerColonnes() {
        colJour.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().jour().format(FormatUtil.FORMAT_DATE)));
        colTotalJour.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.montant(c.getValue().totalTtc())));
        colNbVentesJour.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().nombreVentes())));

        colProduit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().designation()));
        colQuantite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().quantiteTotale().toPlainString()));
        colCaProduit.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.montant(c.getValue().chiffreAffairesTtc())));
    }

    /**
     * Bouton "Generer" : calcule et affiche le rapport a l'ecran.
     * Ne touche pas au PDF -- c'est le role du bouton "Exporter en PDF"
     * separe (exporterPdfRapport()), comme Planning.exporterPdfSemaine()
     * est distinct de la vue hebdomadaire.
     */
    @FXML
    private void genererRapport() {
        LocalDate debut = dateDebut.getValue();
        LocalDate fin = dateFin.getValue();
        if (debut == null || fin == null || debut.isAfter(fin)) {
            DialogueUtil.afficherAvertissement("Periode invalide", "Verifiez que la date de debut precede la date de fin.");
            return;
        }

        try {
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

            // Conserve pour le bouton "Exporter en PDF", sans refaire la requete.
            derniereSynthese = synthese;
            dernierParJour = parJour;
            dernierTopProduits = topProduits;
            derniereDateDebut = debut;
            derniereDateFin = fin;

        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", "Impossible de generer le rapport : " + e.getMessage());
        }
    }

    /**
     * Bouton "Exporter en PDF" : utilise le dernier rapport genere.
     * FileChooser pour choisir l'emplacement, export synchrone, pas
     * d'ouverture automatique -- identique au pattern Planning.
     */
    @FXML
    private void exporterPdfRapport() {
        if (derniereSynthese == null) {
            DialogueUtil.afficherAvertissement("Aucun rapport", "Generez d'abord un rapport avant de l'exporter.");
            return;
        }

        FileChooser selecteur = new FileChooser();
        selecteur.setTitle("Exporter le rapport en PDF");
        selecteur.setInitialFileName(
                "rapport_" + derniereDateDebut.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                        + "_" + derniereDateFin.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf");
        selecteur.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

        File fichier = selecteur.showSaveDialog(tableVentesParJour.getScene().getWindow());
        if (fichier == null) {
            return;
        }

        try {
            RapportPdfExporter.exporter(fichier, derniereDateDebut, derniereDateFin,
                    derniereSynthese, dernierParJour, dernierTopProduits);
            DialogueUtil.afficherAvertissement("Export reussi", "Le rapport a ete exporte vers :\n" + fichier.getAbsolutePath());
        } catch (IOException e) {
            DialogueUtil.afficherErreur("Erreur d'export", "Impossible de generer le PDF : " + e.getMessage());
        }
    }
}