package com.supermarche.controller;

import com.supermarche.config.AppConfig;
import com.supermarche.config.ContexteApplication;
import com.supermarche.model.Categorie;
import com.supermarche.model.Produit;
import com.supermarche.util.DialogueUtil;
import com.supermarche.util.FormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;



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
    @FXML private TextField champCodePLU;
    @FXML private TextArea champDescription;
    @FXML private TextField champPrixAchat;
    @FXML private TextField champPrixVente;
    @FXML private TextField champTauxTva;
    @FXML private TextField champSeuilAlerte;
    @FXML private ComboBox<String> comboUnite;
    @FXML private ComboBox<Categorie> comboCategorie;
    @FXML private ImageView imageProduit;
    @FXML private Label labelPhoto;

    private Produit produitSelectionne;
    private  String dossierPhotos;
    private File photoSelectionnee;

    @FXML
    public void initialize() {
        configurerColonnes();
        comboUnite.setItems(FXCollections.observableArrayList("unite", "kg", "litre", "lot"));
        comboUnite.getSelectionModel().selectFirst();

        chargerCategories();

        tableProduits.getSelectionModel().selectedItemProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                chargerDansFormulaire(nouveau);
            }
        });
        dossierPhotos = AppConfig.getInstance().getDossierPhotos();
        rechercher();
        chargerPhoto(produitSelectionne);
    }

    /**
     * Charge la liste complete des categories (categories de tete ET
     * sous-categories) depuis la base, pour que le formulaire produit
     * propose toujours les categories a jour -- avant, cette liste etait
     * codee en dur et ne montrait jamais les sous-categories creees apres
     * coup (Fruits, Legumes, etc.).
     */
    private void chargerCategories() {
        List<Categorie> categories = ContexteApplication.getInstance()
                .getProduitService().listerCategories();
        comboCategorie.setItems(FXCollections.observableArrayList(categories));
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
        champCodePLU.clear();
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
        champCodePLU.setText(p.getPLU());
        champDesignation.setText(p.getDesignation());
        champDescription.setText(p.getDescription());
        champPrixAchat.setText(p.getPrixAchatHt().toPlainString());
        champPrixVente.setText(p.getPrixVenteTtc().toPlainString());
        champTauxTva.setText(p.getTauxTva().toPlainString());
        champSeuilAlerte.setText(String.valueOf(p.getSeuilAlerteStock()));
        comboUnite.getSelectionModel().select(p.getUnite());
        comboCategorie.getSelectionModel().select(p.getCategorie());
        chargerPhoto(p);
    }

    @FXML
    private void enregistrerProduit() {
        try {
            Produit p = (produitSelectionne != null) ? produitSelectionne : new Produit();
            p.setCodeBarre(champCodeBarre.getText());
            p.setDesignation(champDesignation.getText());
            p.setPLU(champCodePLU.getText());
            p.setDescription(champDescription.getText());
            p.setPrixAchatHt(new BigDecimal(champPrixAchat.getText().replace(",", ".")));
            p.setPrixVenteTtc(new BigDecimal(champPrixVente.getText().replace(",", ".")));
            p.setTauxTva(new BigDecimal(champTauxTva.getText().replace(",", ".")));
            p.setSeuilAlerteStock(Integer.parseInt(champSeuilAlerte.getText()));
            p.setUnite(comboUnite.getValue());
            p.setCategorie(comboCategorie.getValue());
            p.setActif(true);
            if (photoSelectionnee != null) {
                Path destination = Paths.get(dossierPhotos, p.getId() + ".jpg");
                Files.copy(photoSelectionnee.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            }
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
        } catch (IOException e) {
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
    private void chargerPhoto(Produit produit) {
    if (produit == null) {
        imageProduit.setImage(null);
        labelPhoto.setVisible(true);
        return;
    }
    File photo = new File(dossierPhotos, produit.getId() + ".jpg");
        if (photo.exists()) {
            labelPhoto.setVisible(false); 
            imageProduit.setImage(new Image(photo.toURI().toString()));
        } else {
            imageProduit.setImage(null); // ou une image par défaut
        }
    }
   @FXML
    private void choisirPhoto() {
        FileChooser chooser = new FileChooser();
        photoSelectionnee = chooser.showOpenDialog(
                imageProduit.getScene().getWindow());
        if (photoSelectionnee != null) {
            imageProduit.setImage(
                new Image(photoSelectionnee.toURI().toString()));
        }
    }
    @FXML
    private void supprimerPhoto() {
        if (produitSelectionne == null) {
            return;
        }
        File photo = new File(dossierPhotos, produitSelectionne.getId() + ".jpg");

        if (photo.exists()) {
            photo.delete();
        }

        imageProduit.setImage(null);
    }
   @FXML
    private void genererCodeBarre() {
        String code;
        do {
            code = genererEAN13();
        } while (ContexteApplication.getInstance()
                .getProduitService()
                .trouverParCodeBarre(code)
                .isPresent());
        champCodeBarre.setText(code);
    }

    private String genererEAN13() {
        String base = "200";
        while (base.length() < 12) {
            base += (int) (Math.random() * 10);
        }
        int somme = 0;
        for (int i = 0; i < 12; i++) {
            int chiffre = Character.getNumericValue(base.charAt(i));

            if (i % 2 == 0) {
                somme += chiffre;
            } else {
                somme += chiffre * 3;
            }
        }
        int cle = (10 - (somme % 10)) % 10;
        return base + cle;
    }
}