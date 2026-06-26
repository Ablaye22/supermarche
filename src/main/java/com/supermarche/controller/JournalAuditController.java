package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.EntreeJournalAudit;
import com.supermarche.util.DialogueUtil;
import com.supermarche.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class JournalAuditController {

    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private TextField champFiltreAction;

    @FXML private TableView<EntreeJournalAudit> tableJournal;
    @FXML private TableColumn<EntreeJournalAudit, String> colDate;
    @FXML private TableColumn<EntreeJournalAudit, String> colUtilisateur;
    @FXML private TableColumn<EntreeJournalAudit, String> colAction;
    @FXML private TableColumn<EntreeJournalAudit, String> colEntite;
    @FXML private TableColumn<EntreeJournalAudit, String> colDetails;

    @FXML
    public void initialize() {
        configurerColonnes();
        dateDebut.setValue(LocalDate.now().minusDays(7));
        dateFin.setValue(LocalDate.now());
        rechercher();
    }

    private void configurerColonnes() {
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getHorodatage() != null ? c.getValue().getHorodatage().format(FormatUtil.FORMAT_DATE_HEURE) : ""));
        colUtilisateur.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNomUtilisateur() != null ? c.getValue().getNomUtilisateur() : "(systeme)"));
        colAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        colEntite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEntite()));
        colDetails.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetails()));
    }

    @FXML
    private void rechercher() {
        LocalDate debut = dateDebut.getValue();
        LocalDate fin = dateFin.getValue();
        if (debut == null || fin == null || debut.isAfter(fin)) {
            DialogueUtil.afficherAvertissement("Periode invalide", "Verifiez que la date de debut precede la date de fin.");
            return;
        }
        try {
            LocalDateTime debutDateTime = LocalDateTime.of(debut, LocalTime.MIN);
            LocalDateTime finDateTime = LocalDateTime.of(fin, LocalTime.MAX);
            String action = champFiltreAction.getText();

            List<EntreeJournalAudit> resultats = ContexteApplication.getInstance().getAuditService()
                    .rechercher(debutDateTime, finDateTime, (action == null || action.isBlank()) ? null : action.trim());
            tableJournal.setItems(FXCollections.observableArrayList(resultats));
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Acces refuse", e.getMessage());
        }
    }
}
