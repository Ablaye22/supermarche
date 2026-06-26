package com.supermarche.controller;

import com.supermarche.config.ContexteApplication;
import com.supermarche.model.Produit;
import com.supermarche.util.DialogueUtil;
import com.supermarche.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.List;

public class ProduitsController {

    @FXML private TextField champRecherche;
    @FXML private TableView<Produit> tableProduits;
    @FXML private TableColumn<Produit, String> colCodeBarre;
    @FXML private TableColumn<Produit, String> colDesignation;
    @FXML private TableColumn<Produit, String> colPrixAchat;
    @FXML private TableColumn<Produit, String> colPrixVente;
    @FXML private TableColumn<Produit, String> colActif;

    @FXML private Label labelTitreFormulaire;
    @FXML private TextField champCodeBarre;
    @FXML private TextField champDesignation;
    @FXML private TextArea champDescription;
    @FXML private TextField champPrixAchat;
    @FXML private TextField champPrixVente;
    @FXML private TextField champTauxTva;
    @FXML private TextField champSeuilAlerte;
    @FXML private ComboBox<String> comboUnite;

    private Produit produitSelectionne;

    @FXML
    public void initialize() {
        configurerColonnes();
        comboUnite.setItems(FXCollections.observableArrayList("unite", "kg", "litre", "lot"));
        comboUnite.getSelectionModel().selectFirst();

        tableProduits.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                chargerDansFormulaire(nouveau);
            }
        });

        rechercher();
    }

    private void configurerColonnes() {
        colCodeBarre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodeBarre()));
        colDesignation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDesignation()));
        colPrixAchat.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.montant(c.getValue().getPrixAchatHt())));
        colPrixVente.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.montant(c.getValue().getPrixVenteTtc())));
        colActif.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActif() ? "Oui" : "Non"));
    }

    @FXML
    private void rechercher() {
        String motCle = champRecherche.getText();
        List<Produit> resultats = (motCle == null || motCle.isBlank())
                ? ContexteApplication.getInstance().getProduitService().listerTous()
                : ContexteApplication.getInstance().getProduitService().rechercher(motCle);
        tableProduits.setItems(FXCollections.observableArrayList(resultats));
    }

    @FXML
    private void nouveauProduit() {
        produitSelectionne = null;
        labelTitreFormulaire.setText("Nouveau produit");
        champCodeBarre.clear();
        champDesignation.clear();
        champDescription.clear();
        champPrixAchat.clear();
        champPrixVente.clear();
        champTauxTva.setText("20");
        champSeuilAlerte.setText("5");
        comboUnite.getSelectionModel().selectFirst();
        tableProduits.getSelectionModel().clearSelection();
    }

    private void chargerDansFormulaire(Produit p) {
        produitSelectionne = p;
        labelTitreFormulaire.setText("Modifier : " + p.getDesignation());
        champCodeBarre.setText(p.getCodeBarre());
        champDesignation.setText(p.getDesignation());
        champDescription.setText(p.getDescription());
        champPrixAchat.setText(p.getPrixAchatHt().toPlainString());
        champPrixVente.setText(p.getPrixVenteTtc().toPlainString());
        champTauxTva.setText(p.getTauxTva().toPlainString());
        champSeuilAlerte.setText(String.valueOf(p.getSeuilAlerteStock()));
        comboUnite.getSelectionModel().select(p.getUnite());
    }

    @FXML
    private void enregistrerProduit() {
        try {
            Produit p = (produitSelectionne != null) ? produitSelectionne : new Produit();
            p.setCodeBarre(champCodeBarre.getText());
            p.setDesignation(champDesignation.getText());
            p.setDescription(champDescription.getText());
            p.setPrixAchatHt(new BigDecimal(champPrixAchat.getText().replace(",", ".")));
            p.setPrixVenteTtc(new BigDecimal(champPrixVente.getText().replace(",", ".")));
            p.setTauxTva(new BigDecimal(champTauxTva.getText().replace(",", ".")));
            p.setSeuilAlerteStock(Integer.parseInt(champSeuilAlerte.getText()));
            p.setUnite(comboUnite.getValue());
            p.setActif(true);

            if (produitSelectionne != null) {
                ContexteApplication.getInstance().getProduitService().modifierProduit(p);
                DialogueUtil.afficherInfo("Produit modifie", "Les modifications ont ete enregistrees.");
            } else {
                ContexteApplication.getInstance().getProduitService().creerProduit(p);
                DialogueUtil.afficherInfo("Produit cree", "Le produit a ete ajoute au catalogue.");
            }
            rechercher();
            nouveauProduit();
        } catch (NumberFormatException e) {
            DialogueUtil.afficherAvertissement("Valeur invalide", "Verifiez que les prix, le taux de TVA et le seuil sont des nombres valides.");
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }

    @FXML
    private void desactiverProduit() {
        if (produitSelectionne == null) {
            DialogueUtil.afficherAvertissement("Aucun produit selectionne", "Selectionnez un produit a desactiver.");
            return;
        }
        if (!DialogueUtil.confirmer("Desactiver le produit",
                "Voulez-vous vraiment desactiver \"" + produitSelectionne.getDesignation() + "\" ? "
                        + "Il ne sera plus propose a la vente mais restera visible dans l'historique.")) {
            return;
        }
        try {
            ContexteApplication.getInstance().getProduitService().desactiverProduit(produitSelectionne.getId());
            rechercher();
            nouveauProduit();
        } catch (Exception e) {
            DialogueUtil.afficherErreur("Erreur", e.getMessage());
        }
    }
}
