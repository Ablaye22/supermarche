package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.Employe;
import com.supermarche.model.Role;
import com.supermarche.model.Utilisateur;
import com.supermarche.util.DialogueUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.util.List;

public class UtilisateursController {

    @FXML private TableView<Utilisateur> tableUtilisateurs;
    @FXML private TableColumn<Utilisateur, String> colId;
    @FXML private TableColumn<Utilisateur,String> champIdUtilisateur;
    @FXML private TableColumn<Utilisateur, String> colNomComplet;
    @FXML private TableColumn<Utilisateur, String> colNomUtilisateur;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colStatut;

    @FXML private Label LabelTitreFormulaire;
    @FXML private Label IdLabel;
    @FXML private TextField champId;
    @FXML private TextField champNom;
    @FXML private TextField champPrenom;
    @FXML private TextField champEmail;
    @FXML private TextField champNomUtilisateur;
    @FXML private PasswordField champMotDePasse;
    @FXML private ComboBox<Role> comboRole;
    @FXML private Button creerModifier;

    private Utilisateur utilisateurSelectionne = null;

    @FXML
    public void initialize() {
        configurerColonnes();
        chargerRoles();
        chargerListe();
         tableUtilisateurs.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, ancien, nouveau) -> {
                if (nouveau != null) {
                    chargerUtilisateurDansFormulaire(nouveau);
                }
            });
    }

    private void configurerColonnes() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getEmploye().getId())));
        champIdUtilisateur.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colNomComplet.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmploye().getNomComplet()));
        colNomUtilisateur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomUtilisateur()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().getLibelle()));
        colStatut.setCellValueFactory(c -> {
            Utilisateur u = c.getValue();
            String statut = !u.isActif() ? "Inactif" : (u.isCompteVerrouille() ? "Verrouille" : "Actif");
            return new SimpleStringProperty(statut);
        });
    }

    private void chargerRoles() {
        List<Role> roles = ContexteApplication.getInstance().getUtilisateurService().listerRoles();
        comboRole.setItems(FXCollections.observableArrayList(roles));
        comboRole.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role role) {
                return role == null ? "" : role.getLibelle();
            }

            @Override
            public Role fromString(String s) {
                return null;
            }
        });
        if (!roles.isEmpty()) {
            comboRole.getSelectionModel().selectFirst();
        }
    }

    private void chargerListe() {
        try {
            List<Utilisateur> utilisateurs = ContexteApplication.getInstance().getUtilisateurService().listerTous();
            tableUtilisateurs.setItems(FXCollections.observableArrayList(utilisateurs));
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Acces refuse", e.getMessage());
        }
    }

    private void creerUtilisateur() {
        try {
            if (champMotDePasse.getText() == null || champMotDePasse.getText().isBlank()) {
                DialogueUtil.afficherAvertissement("Mot de passe requis", "Saisissez un mot de passe initial.");
                return;
            }
            Employe employe = new Employe();
            employe.setNom(champNom.getText());
            employe.setPrenom(champPrenom.getText());
            employe.setEmail(champEmail.getText());
            employe.setDateEmbauche(LocalDate.now());
            employe.setActif(true);

            Role roleChoisi = comboRole.getValue();
            if (roleChoisi == null) {
                DialogueUtil.afficherAvertissement("Role requis", "Selectionnez un role pour ce compte.");
                return;
            }

            ContexteApplication.getInstance().getUtilisateurService().creerUtilisateur(
                    employe, champNomUtilisateur.getText(), champMotDePasse.getText(), roleChoisi.getCode());

            DialogueUtil.afficherInfo("Compte cree", "Le compte utilisateur a ete cree avec succes.");
            viderFormulaire();
            chargerListe();
        } catch (IllegalArgumentException e) {
            DialogueUtil.afficherAvertissement("Saisie invalide", e.getMessage());
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }
    private void modifierUtilisateur()
    {
        try {
            Utilisateur user = new Utilisateur();
            user.setId(Integer.decode(champIdUtilisateur.getText()));
            user.setEmploye( new Employe());
            user.getEmploye().setId(Integer.decode(champId.getText()));
            user.getEmploye().setNom(champNom.getText());
            user.getEmploye().setPrenom(champPrenom.getText());
            user.getEmploye().setEmail(champEmail.getText());
            user.getEmploye().setDateEmbauche(LocalDate.now());
            user.getEmploye().setActif(true);
            user.setNomUtilisateur(champNomUtilisateur.getText());

            Role roleChoisi = comboRole.getValue();
            if (roleChoisi == null) {
                DialogueUtil.afficherAvertissement("Role requis", "Selectionnez un role pour ce compte.");
                return;
            }

            ContexteApplication.getInstance().getUtilisateurService().modifierUtilisateur(user, roleChoisi.getCode());

            DialogueUtil.afficherInfo("Compte cree", "Le compte utilisateur a ete cree avec succes.");
            viderFormulaire();
            chargerListe();
        } catch (IllegalArgumentException e) {
            DialogueUtil.afficherAvertissement("Saisie invalide", e.getMessage());
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }
    @FXML
    private void  enregistrer(){
        if (utilisateurSelectionne == null) {
            creerUtilisateur();
        } else {
            modifierUtilisateur();
        }
    }

    private void viderFormulaire() {
        champNom.clear();
        champPrenom.clear();
        champEmail.clear();
        champNomUtilisateur.clear();
        champMotDePasse.clear();
    }
    private void chargerUtilisateurDansFormulaire(Utilisateur utilisateur) {
        LabelTitreFormulaire.setText("Modifier compte Utilisateur");
        champIdUtilisateur.setText(utilisateur.getId().toString());
        champId.setText(utilisateur.getEmploye().getId().toString());
        utilisateurSelectionne = utilisateur;   
        Employe employe = utilisateur.getEmploye();
        champNom.setText(employe.getNom());
        champPrenom.setText(employe.getPrenom());
        champEmail.setText(employe.getEmail());
        champNomUtilisateur.setText(utilisateur.getNomUtilisateur());
        creerModifier.setText("Modifier le compte");
        champMotDePasse.setText("***************");
        champMotDePasse.setDisable(true);
        comboRole.getSelectionModel().select(utilisateur.getRole());
    }

    @FXML 
    private void nouveauUtilisateur(){
        LabelTitreFormulaire.setText("Nouveau compte Utilisateur"); 
        champId.clear();
        IdLabel.setVisible(false);
        champId.setVisible(false);
        viderFormulaire();
        champMotDePasse.setDisable(false);
        chargerRoles();
    }

    @FXML
    private void activerUtilisateur() {
        changerStatutSelection(true);
    }

    @FXML
    private void desactiverUtilisateur() {
        changerStatutSelection(false);
    }

    private void changerStatutSelection(boolean actif) {
        Utilisateur selectionne = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            DialogueUtil.afficherAvertissement("Aucune selection", "Selectionnez un utilisateur dans la liste.");
            return;
        }
        try {
            ContexteApplication.getInstance().getUtilisateurService().activerOuDesactiver(selectionne.getId(), actif);
            chargerListe();
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }

    @FXML
    private void deverrouillerCompte() {
        Utilisateur selectionne = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            DialogueUtil.afficherAvertissement("Aucune selection", "Selectionnez un utilisateur dans la liste.");
            return;
        }
        try {
            ContexteApplication.getInstance().getUtilisateurService().deverrouillerCompte(selectionne.getId());
            DialogueUtil.afficherInfo("Compte deverrouille", "Le compte a ete deverrouille.");
            chargerListe();
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }
    @FXML
    private void reinitialiserPassword(){
        Utilisateur selectionne = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (selectionne == null) {
            DialogueUtil.afficherAvertissement("Aucune selection", "Selectionnez un utilisateur dans la liste.");
            return;
        }
        try {
            ContexteApplication.getInstance().getUtilisateurService().changerMotDePasse(selectionne.getId());
            DialogueUtil.afficherInfo("Réinitialisation de Mot de passe", "Le Mot de passe a été réinitialiser avec succès");
            chargerListe();
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }
}
