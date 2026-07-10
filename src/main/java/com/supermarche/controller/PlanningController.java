package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.Employe;
import com.supermarche.model.Planning;
import com.supermarche.model.Utilisateur;
import com.supermarche.util.DialogueUtil;
import com.supermarche.util.PlanningPdfExporter;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanningController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATTER_COURT = DateTimeFormatter.ofPattern("dd/MM");
    private static final String[] NOMS_JOURS = {
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    };

    @FXML private ComboBox<Utilisateur> comboRechercheEmploye;
    @FXML private DatePicker dateRecherche;

    @FXML private ComboBox<Utilisateur> comboUtilisateur;
    @FXML private DatePicker champDate;
    @FXML private TextField champHeureDebut;
    @FXML private TextField champHeureFin;
    @FXML private Spinner<Integer> spinnerPause;
    @FXML private TextField champPoste;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TextArea champObservation;
    @FXML private TableView<Planning> tableauPlanning;
    @FXML private TableColumn<Planning, String> colonneEmploye;
    @FXML private TableColumn<Planning, LocalDate> colonneDate;
    @FXML private TableColumn<Planning, String> colonneHoraire;
    @FXML private TableColumn<Planning, String> colonnePoste;
    @FXML private TableColumn<Planning, String> colonneStatut;

    @FXML private Button btnSemainePrecedente;
    @FXML private Button btnSemaineSuivante;
    @FXML private Label labelSemaine;
    @FXML private TextField champRechercheNomGrille;
    @FXML private TableView<Utilisateur> tableauGrilleSemaine;
    @FXML private TableColumn<Utilisateur, String> colonneEmployeGrille;
    @FXML private TableColumn<Utilisateur, Planning> colonneJour0;
    @FXML private TableColumn<Utilisateur, Planning> colonneJour1;
    @FXML private TableColumn<Utilisateur, Planning> colonneJour2;
    @FXML private TableColumn<Utilisateur, Planning> colonneJour3;
    @FXML private TableColumn<Utilisateur, Planning> colonneJour4;
    @FXML private TableColumn<Utilisateur, Planning> colonneJour5;
    @FXML private TableColumn<Utilisateur, Planning> colonneJour6;

    private Planning planningSelectionne;

    private LocalDate lundiSemaineAffichee;
    private List<TableColumn<Utilisateur, Planning>> colonnesJours;
    private final Map<Integer, Map<LocalDate, Planning>> planningParEmployeEtDate = new HashMap<>();
    private final ObservableList<Utilisateur> employesGrille = FXCollections.observableArrayList();
    private FilteredList<Utilisateur> employesFiltres;

    @FXML public void initialize() {

        spinnerPause.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 180, 30));
        comboStatut.setItems(FXCollections.observableArrayList("Prévu", "Présent", "Absent", "Congé", "Repos"));

        configurerColonnes();
        chargerEmployes();
        chargerPlannings();

        tableauPlanning.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
                    if (nouveau != null) {
                        afficherPlanning(nouveau);
                    }
                });

        initialiserVueHebdomadaire();
    }

    private void configurerColonnes() {
        colonneEmploye.setCellValueFactory(data -> {
            Utilisateur utilisateur = data.getValue().getEmploye();
            String nomComplet = "";
            if (utilisateur != null && utilisateur.getEmploye() != null) {
                nomComplet = utilisateur.getEmploye().getPrenom() + " " + utilisateur.getEmploye().getNom();
            }
            return new SimpleStringProperty(nomComplet);
        });

        colonneDate.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getDate()));

        colonneHoraire.setCellValueFactory(data -> {
            Planning p = data.getValue();
            String horaire = p.getHeureDebut() + " - " + p.getHeureFin();
            return new SimpleStringProperty(horaire);
        });

        colonnePoste.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPoste()));

        colonneStatut.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatut()));
    }

    private void chargerEmployes() {
        var employes = FXCollections.observableArrayList(
                ContexteApplication.getInstance().getUtilisateurService().listerTous());
        comboUtilisateur.setItems(employes);
        comboRechercheEmploye.setItems(employes);
    }

    private void chargerPlannings() {
        tableauPlanning.setItems(FXCollections.observableArrayList(
                ContexteApplication.getInstance().getPlanningService().listerTous()));
    }

    private void afficherPlanning(Planning planning) {
        planningSelectionne = planning;
        comboUtilisateur.setValue(planning.getEmploye());
        champDate.setValue(planning.getDate());
        champHeureDebut.setText(planning.getHeureDebut().toString());
        champHeureFin.setText(planning.getHeureFin().toString());
        spinnerPause.getValueFactory().setValue(planning.getPauseMinutes());
        champPoste.setText(planning.getPoste());
        comboStatut.setValue(planning.getStatut());
        champObservation.setText(planning.getObservation());
    }

    @FXML
    private void enregistrerPlanning() {
        try {
            Planning planning = new Planning();
            if (planningSelectionne != null) {
                planning.setId(planningSelectionne.getId());
            }
            planning.setEmploye(comboUtilisateur.getValue());
            planning.setDate(champDate.getValue());
            planning.setHeureDebut(java.time.LocalTime.parse(champHeureDebut.getText()));
            planning.setHeureFin(java.time.LocalTime.parse(champHeureFin.getText()));
            planning.setPauseMinutes(spinnerPause.getValue());
            planning.setPoste(champPoste.getText());
            planning.setStatut(comboStatut.getValue());
            planning.setObservation(champObservation.getText());
            if (planningSelectionne == null) {
                ContexteApplication.getInstance().getPlanningService().creerPlanning(planning);
            } else {
                ContexteApplication.getInstance().getPlanningService().modifierPlanning(planning);
            }
            chargerPlannings();
            chargerVueHebdomadaire();
            nouveauPlanning();
        } catch (Exception e) {
            DialogueUtil.afficherErreur(
                    "Erreur",
                    e.getMessage());
        }
    }

    @FXML
    private void supprimerPlanning() {

        if (planningSelectionne == null) {
            DialogueUtil.afficherAvertissement("Planning", "Sélectionnez un planning.");
            return;
        }
        if (DialogueUtil.confirmer("Confirmation", "Supprimer ce planning ?")) {
            ContexteApplication.getInstance().getPlanningService().supprimerPlanning(planningSelectionne.getId());
            chargerPlannings();
            chargerVueHebdomadaire();
            nouveauPlanning();
        }
    }

    @FXML
    private void nouveauPlanning() {

        planningSelectionne = null;
        comboUtilisateur.setValue(null);
        champDate.setValue(LocalDate.now());
        champHeureDebut.clear();
        champHeureFin.clear();
        spinnerPause.getValueFactory().setValue(30);
        champPoste.clear();
        comboStatut.setValue("Prévu");
        champObservation.clear();
        tableauPlanning.getSelectionModel().clearSelection();
    }

    @FXML
    private void rechercherPlanning() {

        Utilisateur employe = comboRechercheEmploye.getValue();
        LocalDate date = dateRecherche.getValue();

        try {
            if (employe != null && date != null) {
                var resultats = ContexteApplication.getInstance().getPlanningService()
                        .listerParEmploye(employe.getId());
                resultats.removeIf(p -> !p.getDate().equals(date));
                tableauPlanning.setItems(FXCollections.observableArrayList(resultats));
            } else if (employe != null) {
                tableauPlanning.setItems(FXCollections.observableArrayList(
                        ContexteApplication.getInstance().getPlanningService().listerParEmploye(employe.getId())));
            } else if (date != null) {
                tableauPlanning.setItems(FXCollections.observableArrayList(
                        ContexteApplication.getInstance().getPlanningService().listerParDate(date)));
            } else {
                chargerPlannings();
            }
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }

    // ======================== VUE HEBDOMADAIRE ========================

    private void initialiserVueHebdomadaire() {

        colonnesJours = List.of(
                colonneJour0, colonneJour1, colonneJour2,
                colonneJour3, colonneJour4, colonneJour5, colonneJour6);

        colonneEmployeGrille.setCellValueFactory(data -> {
            Employe e = data.getValue().getEmploye();
            String nom = e == null ? "" : e.getPrenom() + " " + e.getNom();
            return new SimpleStringProperty(nom);
        });

        for (int i = 0; i < colonnesJours.size(); i++) {
            final int offset = i;
            TableColumn<Utilisateur, Planning> colonne = colonnesJours.get(i);

            colonne.setCellValueFactory(data -> {
                Utilisateur u = data.getValue();
                LocalDate jour = lundiSemaineAffichee.plusDays(offset);
                if (u.getEmploye() == null) {
                    return new SimpleObjectProperty<>(null);
                }
                Map<LocalDate, Planning> parDate = planningParEmployeEtDate.get(u.getEmploye().getId());
                Planning p = (parDate == null) ? null : parDate.get(jour);
                return new SimpleObjectProperty<>(p);
            });

            colonne.setCellFactory(col -> new TableCell<Utilisateur, Planning>() {
                @Override
                protected void updateItem(Planning planning, boolean empty) {
                    super.updateItem(planning, empty);
                    if (empty || planning == null) {
                        setGraphic(null);
                        setText(null);
                        setStyle("");
                    } else {
                        Label contenu = new Label(
                                planning.getHeureDebut() + " - " + planning.getHeureFin()
                                        + "\n" + planning.getPoste());
                        contenu.setWrapText(true);
                        setGraphic(contenu);
                        setText(null);
                        setStyle(styleParStatut(planning.getStatut()));
                    }
                }
            });
        }

        employesFiltres = new FilteredList<>(employesGrille, u -> true);
        tableauGrilleSemaine.setItems(employesFiltres);

        champRechercheNomGrille.textProperty().addListener((obs, ancien, nouveau) -> {
            String texte = (nouveau == null) ? "" : nouveau.trim().toLowerCase();
            employesFiltres.setPredicate(u -> {
                if (texte.isEmpty()) {
                    return true;
                }
                Employe e = u.getEmploye();
                if (e == null) {
                    return false;
                }
                String direct = (e.getNom() + " " + e.getPrenom()).toLowerCase();
                String inverse = (e.getPrenom() + " " + e.getNom()).toLowerCase();
                return direct.contains(texte) || inverse.contains(texte);
            });
        });

        lundiSemaineAffichee = LocalDate.now().with(DayOfWeek.MONDAY);
        chargerVueHebdomadaire();
    }

    private void chargerVueHebdomadaire() {

        LocalDate dimanche = lundiSemaineAffichee.plusDays(6);
        labelSemaine.setText("Semaine du " + FORMATTER.format(lundiSemaineAffichee)
                + " au " + FORMATTER.format(dimanche));

        List<Planning> plannings;
        try {
            plannings = ContexteApplication.getInstance().getPlanningService()
                    .listerEntreDates(lundiSemaineAffichee, dimanche);
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
            plannings = List.of();
        }

        planningParEmployeEtDate.clear();
        for (Planning p : plannings) {
            if (p.getEmploye() == null || p.getEmploye().getEmploye() == null
                    || p.getEmploye().getEmploye().getId() == null) {
                continue;
            }
            planningParEmployeEtDate
                    .computeIfAbsent(p.getEmploye().getEmploye().getId(), k -> new HashMap<>())
                    .put(p.getDate(), p);
        }

        employesGrille.setAll(
                ContexteApplication.getInstance().getUtilisateurService().listerTous());

        for (int i = 0; i < colonnesJours.size(); i++) {
            LocalDate jour = lundiSemaineAffichee.plusDays(i);
            colonnesJours.get(i).setText(NOMS_JOURS[i] + " " + FORMATTER_COURT.format(jour));
        }

        tableauGrilleSemaine.refresh();
        mettreAJourEtatBoutons();
    }

    @FXML
    private void semainePrecedente() {
        LocalDate lundiCourant = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate nouveauLundi = lundiSemaineAffichee.minusWeeks(1);
        if (!nouveauLundi.isBefore(lundiCourant)) {
            lundiSemaineAffichee = nouveauLundi;
            chargerVueHebdomadaire();
        }
    }

    @FXML
    private void semaineSuivante() {
        lundiSemaineAffichee = lundiSemaineAffichee.plusWeeks(1);
        chargerVueHebdomadaire();
    }

    private void mettreAJourEtatBoutons() {
        LocalDate lundiCourant = LocalDate.now().with(DayOfWeek.MONDAY);
        btnSemainePrecedente.setDisable(lundiSemaineAffichee.equals(lundiCourant));
    }

    @FXML
    private void exporterPdfSemaine() {

        FileChooser selecteur = new FileChooser();
        selecteur.setTitle("Exporter le planning en PDF");
        selecteur.setInitialFileName(
                "planning_semaine_" + lundiSemaineAffichee.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf");
        selecteur.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

        File fichier = selecteur.showSaveDialog(tableauGrilleSemaine.getScene().getWindow());
        if (fichier == null) {
            return;
        }

        try {
            List<Utilisateur> employesAExporter = new ArrayList<>(employesFiltres);
            PlanningPdfExporter.exporter(fichier, lundiSemaineAffichee, employesAExporter, planningParEmployeEtDate);
            DialogueUtil.afficherAvertissement("Export reussi", "Le planning a ete exporte vers :\n" + fichier.getAbsolutePath());
        } catch (IOException e) {
            DialogueUtil.afficherErreur("Erreur d'export", "Impossible de generer le PDF : " + e.getMessage());
        }
    }

    private String styleParStatut(String statut) {
        if (statut == null) {
            return "";
        }
        return switch (statut) {
            case "Présent" -> "-fx-background-color: #d4edda;";
            case "Absent" -> "-fx-background-color: #f8d7da;";
            case "Congé" -> "-fx-background-color: #fff3cd;";
            case "Repos" -> "-fx-background-color: #e2e3e5;";
            default -> "-fx-background-color: #d1ecf1;";
        };
    }
}