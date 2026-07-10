package com.supermarche.controller;

import com.supermarche.config.AppConfig;
import com.supermarche.config.ContexteApplication;
import com.supermarche.exception.SupermarcheException;
import com.supermarche.model.Caisse;
import com.supermarche.model.Categorie;
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
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.awt.Toolkit;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.supermarche.security.PasswordHasher;
import javafx.animation.PauseTransition;
import javafx.scene.control.PasswordField;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

public class CaisseController {

    @FXML private VBox panneauSession;
    @FXML private ComboBox<Caisse> comboCaisse;
    @FXML private Label labelStatutSession;
    @FXML private TextField champFondOuverture;
    @FXML private Button boutonOuvrirSession;
    @FXML private Button boutonFermerSession;

    // Bascule entre les deux modes
    @FXML private Button boutonModePave;
    @FXML private Button boutonModeCategories;

    // Panneau pave numerique
    @FXML private VBox panneauPave;
    @FXML private Label labelAfficheurPave;
    @FXML private Label labelDernierProduitAjoute;

    // Panneau categories
    @FXML private VBox panneauCategories;
    @FXML private FlowPane conteneurBoutonsCategories;
    @FXML private Label labelCategorieSelectionnee;
    @FXML private TabPane tabsSousCategories;
    @FXML private FlowPane grilleProduitsCategorie;

    // Panier et paiement (communs aux deux modes)
    @FXML private TableView<LignePanier> tablePanier;
    @FXML private TableColumn<LignePanier, String> colPanierDesignation;
    @FXML private TableColumn<LignePanier, Number> colPanierQuantite;
    @FXML private TableColumn<LignePanier, String> colPanierPrixUnitaire;
    @FXML private TableColumn<LignePanier, String> colPanierTotal;
    @FXML private TextField champCarteFidelite;
    @FXML private Label labelTotalTicket;
    @FXML private ComboBox<ModePaiement> comboModePaiement;
    @FXML private TextField champMontantPaiement;
    @FXML private Button boutonDeconnexion;
    @FXML private VBox panneauVerrouillage;
    @FXML private PasswordField champMotDePasseVerrou;
    @FXML private Label labelNomCaissierVerrou;
    @FXML private Label labelErreurVerrou;
    @FXML private Button boutonVerrouiller;
    @FXML private Label labelDate;
    @FXML private Label labelHeure;

    private final ObservableList<LignePanier> panier = FXCollections.observableArrayList();
    private SessionCaisse sessionOuverte;
    private final StringBuilder saisieCourante = new StringBuilder();
    private static final int DELAI_VERROUILLAGE_MINUTES = 1;
    private PauseTransition minuterieVerrouillage;
    private List<Categorie> toutesLesCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        demarrerHorloge();
        configurerColonnes();

        // Les appels DB ci-dessous ne doivent jamais empecher le reste de
        // l'ecran de se charger : une base non disponible ou une table vide
        // ne doit pas planter tout le FXMLLoader.load(). On isole chaque
        // etape a risque avec un try/catch qui affiche l'erreur reelle.
        try {
            chargerCaissesDisponibles();
        } catch (Exception e) {
            e.printStackTrace();
            DialogueUtil.afficherErreur("Erreur de chargement",
                    "Impossible de charger la liste des caisses : " + e.getMessage());
        }

        comboModePaiement.setItems(FXCollections.observableArrayList(ModePaiement.values()));
        comboModePaiement.getSelectionModel().select(ModePaiement.ESPECES);
        tablePanier.setItems(panier);
        comboCaisse.valueProperty().addListener((obs, ancienne, nouvelle) -> verifierSessionPourCaisse());
        rafraichirEtatBoutonsSession();
        rafraichirAfficheurPave();

        try {
            chargerBoutonsCategories();
        } catch (Exception e) {
            e.printStackTrace();
            DialogueUtil.afficherErreur("Erreur de chargement",
                    "Impossible de charger les categories : " + e.getMessage());
        }

        minuterieVerrouillage = new PauseTransition(
                Duration.minutes(DELAI_VERROUILLAGE_MINUTES));
        minuterieVerrouillage.setOnFinished(e -> verrouillerCaisse());

        panneauVerrouillage.sceneProperty().addListener((obs, ancScene, nouvelleScene) -> {
            if (nouvelleScene != null) {
                nouvelleScene.setOnMouseMoved(e -> reinitialiserMinuterie());
                nouvelleScene.setOnKeyPressed(e -> reinitialiserMinuterie());
            }
        });
    }

    private void demarrerHorloge() {
        DateTimeFormatter formatDate =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatHeure =
                DateTimeFormatter.ofPattern("HH:mm:ss");
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    labelDate.setText(
                            LocalDate.now().format(formatDate));
                    labelHeure.setText(
                            LocalTime.now().format(formatHeure));
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // ------------------------------------------------------------------
    // Bascule entre le mode pave numerique et le mode categories
    // ------------------------------------------------------------------

    @FXML
    private void activerModePave() {
        panneauPave.setVisible(true);
        panneauPave.setManaged(true);
        panneauCategories.setVisible(false);
        panneauCategories.setManaged(false);
        boutonModePave.getStyleClass().remove("bouton-secondaire");
        boutonModePave.getStyleClass().add("bouton-principal");
        boutonModeCategories.getStyleClass().remove("bouton-principal");
        boutonModeCategories.getStyleClass().add("bouton-secondaire");
    }

    @FXML
    private void activerModeCategories() {
        panneauCategories.setVisible(true);
        panneauCategories.setManaged(true);
        panneauPave.setVisible(false);
        panneauPave.setManaged(false);
        boutonModeCategories.getStyleClass().remove("bouton-secondaire");
        boutonModeCategories.getStyleClass().add("bouton-principal");
        boutonModePave.getStyleClass().remove("bouton-principal");
        boutonModePave.getStyleClass().add("bouton-secondaire");
    }

    // ------------------------------------------------------------------
    // Panneau categories : chargement des boutons et de la grille produits
    // ------------------------------------------------------------------

    /**
     * Charge les boutons de categories : seules les categories "de tete"
     * (sans parent, id_sous_categorie == null) sont affichees dans la
     * barre. Les categories enfants apparaissent en drill-down quand on
     * clique sur leur parent (voir afficherProduitsCategorie).
     */
    private void chargerBoutonsCategories() {
        toutesLesCategories = ContexteApplication.getInstance()
                .getProduitService().listerCategories();
        conteneurBoutonsCategories.getChildren().clear();

        for (Categorie categorie : toutesLesCategories) {
            if (categorie.getIdSousCategorie() != null) {
                continue; // ce n'est pas une categorie de tete, elle apparaitra en drill-down
            }
            Button bouton = new Button(categorie.getNom());
            bouton.getStyleClass().add("bouton-secondaire");
            bouton.setOnAction(e -> afficherProduitsCategorie(categorie));
            conteneurBoutonsCategories.getChildren().add(bouton);
        }
    }

    /**
     * Si la categorie cliquee a des enfants, on les presente sous forme
     * d'onglets (plus un onglet "Tous" pour les produits restes rattaches
     * directement au parent, ex: anciens produits "Fruits et legumes" pas
     * encore reclasses). Sinon, pas d'onglets : on affiche directement les
     * produits de la categorie.
     */
    private void afficherProduitsCategorie(Categorie categorie) {
        labelCategorieSelectionnee.setText(categorie.getNom());

        List<Categorie> enfants = toutesLesCategories.stream()
                .filter(c -> categorie.getId().equals(c.getIdSousCategorie()))
                .toList();

        tabsSousCategories.getTabs().clear();

        if (enfants.isEmpty()) {
            tabsSousCategories.setVisible(false);
            tabsSousCategories.setManaged(false);
            afficherProduitsDeCategorie(categorie);
            return;
        }

        Tab ongletTous = new Tab("Tous");
        ongletTous.setOnSelectionChanged(e -> {
            if (ongletTous.isSelected()) {
                afficherProduitsDeCategorie(categorie);
            }
        });
        tabsSousCategories.getTabs().add(ongletTous);

        for (Categorie enfant : enfants) {
            Tab onglet = new Tab(enfant.getNom());
            onglet.setOnSelectionChanged(e -> {
                if (onglet.isSelected()) {
                    afficherProduitsDeCategorie(enfant);
                }
            });
            tabsSousCategories.getTabs().add(onglet);
        }

        tabsSousCategories.setVisible(true);
        tabsSousCategories.setManaged(true);
        tabsSousCategories.getSelectionModel().selectFirst();
    }

    /** Affiche les produits d'une categorie (feuille ou parente) dans la grille. */
    private void afficherProduitsDeCategorie(Categorie categorie) {
        grilleProduitsCategorie.getChildren().clear();

        List<Produit> produits = ContexteApplication.getInstance()
                .getProduitService().listerParCategorie(categorie.getId());

        String dossierPhotos = AppConfig.getInstance().getDossierPhotos();

        for (Produit produit : produits) {
            VBox carte = creerCarteProduit(produit, dossierPhotos);
            grilleProduitsCategorie.getChildren().add(carte);
        }
    }

    /**
     * Cree une carte visuelle pour un produit : photo (ou icone par
     * defaut si le fichier est absent) + nom + prix. Un clic ajoute le
     * produit directement au panier avec une quantite de 1.
     */
    private VBox creerCarteProduit(Produit produit, String dossierPhotos) {
        VBox carte = new VBox(6);
        carte.setAlignment(Pos.CENTER);
        carte.setPrefWidth(170);
        carte.setPrefHeight(170);
        carte.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 8px;
                -fx-padding: 8px;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 4, 0, 0, 2);
                -fx-cursor: hand;
                """);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        String cheminPhoto = dossierPhotos + File.separator + produit.getId() + ".jpg";
        File fichierPhoto = new File(cheminPhoto);
        if (fichierPhoto.exists()) {
            try {
                Image image = new Image(fichierPhoto.toURI().toString(), 72, 72, true, true);
                imageView.setImage(image);
            } catch (Exception e) {
                imageView.setImage(null);
            }
        }
        // Si pas de photo, on affiche juste le nom et le prix sans planter.

        Label labelNom = new Label(produit.getDesignation());
        labelNom.setWrapText(true);
        labelNom.setMaxWidth(100);
        labelNom.setAlignment(Pos.CENTER);
        labelNom.setStyle("-fx-font-size: 16px; -fx-text-alignment: center;");

        Label labelPrix = new Label(com.supermarche.util.FormatUtil.montant(produit.getPrixVenteTtc()));
        Label labelPLU = new Label(produit.getPLU());
        labelPLU.setStyle("-fx-font-weight: bold; -fx-font-siz: 12px; -fx-text-fill: black;");
        labelPrix.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2E6F95;");

        carte.getChildren().addAll(imageView, labelNom, labelPrix, labelPLU);

        // Survol : legerement surligne
        carte.setOnMouseEntered(e -> carte.setStyle("""
                -fx-background-color: #EAF2F7;
                -fx-background-radius: 8px;
                -fx-padding: 8px;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 3);
                -fx-cursor: hand;
                """));
        carte.setOnMouseExited(e -> carte.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 8px;
                -fx-padding: 8px;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 4, 0, 0, 2);
                -fx-cursor: hand;
                """));

        carte.setOnMouseClicked(e -> {
            if (sessionOuverte == null) {
                DialogueUtil.afficherAvertissement("Session requise",
                        "Ouvrez une session de caisse avant d'enregistrer une vente.");
                return;
            }
            ajouterProduitAuPanier(produit, 1);
            if (labelDernierProduitAjoute != null) {
                labelDernierProduitAjoute.setText("Dernier article : " + produit.getDesignation());
            }
        });

        return carte;
    }

    // ------------------------------------------------------------------
    // Pave numerique
    // ------------------------------------------------------------------

    @FXML
    private void saisirChiffre(javafx.event.ActionEvent evenement) {
        Button bouton = (Button) evenement.getSource();
        if (saisieCourante.length() >= 13) {
            return;
        }
        saisieCourante.append(bouton.getText());
        rafraichirAfficheurPave();
    }

    /** Bouton "00" : ajoute deux zeros d'un coup (pratique pour les centimes). */
    @FXML
    private void saisirDoubleZero() {
        if (saisieCourante.length() <= 11) {
            saisieCourante.append("00");
        }
        rafraichirAfficheurPave();
    }

    /**
     * Bouton "." : ajoute une virgule decimale pour les quantites au poids
     * (ex: 1.5 kg). N'est utilisable que dans le mode "Appliquer quantite",
     * pas pour un code-barres (un code-barres ne contient jamais de virgule).
     * On empeche d'en saisir deux.
     */
    @FXML
    private void saisirVirgule() {
        if (!saisieCourante.toString().contains(".") && saisieCourante.length() < 12) {
            if (saisieCourante.length() == 0) {
                saisieCourante.append("0");
            }
            saisieCourante.append(".");
        }
        rafraichirAfficheurPave();
    }

    @FXML
    private void effacerSaisie() {
        saisieCourante.setLength(0);
        rafraichirAfficheurPave();
    }

    @FXML
    private void effacerDernierChiffre() {
        if (saisieCourante.length() > 0) {
            saisieCourante.deleteCharAt(saisieCourante.length() - 1);
        }
        rafraichirAfficheurPave();
    }

    private void rafraichirAfficheurPave() {
        labelAfficheurPave.setText(saisieCourante.length() == 0 ? "0" : saisieCourante.toString());
    }

    @FXML
    private void validerCodeBarrePave() {
        String codeBarre = saisieCourante.toString();
        if (codeBarre.isBlank()) {
            DialogueUtil.afficherAvertissement("Saisie vide", "Saisissez un code-barres avant de valider.");
            return;
        }
        if (sessionOuverte == null) {
            DialogueUtil.afficherAvertissement("Session requise",
                    "Ouvrez une session de caisse avant d'enregistrer une vente.");
            return;
        }
        Optional<Produit> produit = ContexteApplication.getInstance()
                .getProduitService().trouverParCodeBarre(codeBarre);
        if (produit.isEmpty()) {
            DialogueUtil.afficherAvertissement("Produit introuvable",
                    "Aucun produit ne correspond au code-barres " + codeBarre + ".");
            effacerSaisie();
            return;
        }
        ajouterProduitAuPanier(produit.get(), 1);
        if (labelDernierProduitAjoute != null) {
            labelDernierProduitAjoute.setText("Dernier article : " + produit.get().getDesignation());
        }
        effacerSaisie();
    }

    @FXML
    private void validercodePLU(){
        String codePLU = saisieCourante.toString();
        if(codePLU.isBlank()){
            DialogueUtil.afficherAvertissement("Saisie vide", "Saisissez un code PLU avant de valider ");
            return ;
        }
        if (sessionOuverte == null) {
            DialogueUtil.afficherAvertissement("Session requise",
                    "Ouvrez une session de caisse avant d'enregistrer une vente.");
            return;
        }
        Optional<Produit> produit = ContexteApplication.getInstance().getProduitService().trouverParCodePLU(codePLU);
        if (produit.isEmpty()) {
            DialogueUtil.afficherAvertissement("Produit introuvable",
                    "Aucun produit ne correspond au code-PLU " + codePLU + ".");
            effacerSaisie();
            return;
        }
        ajouterProduitAuPanier(produit.get(), 1);
        effacerSaisie();
        if (labelDernierProduitAjoute != null) {
            labelDernierProduitAjoute.setText("Dernier article : " + produit.get().getDesignation());
        }
    }

    @FXML
    private void appliquerQuantitePave() {
        String saisie = saisieCourante.toString();
        if (saisie.isBlank() || saisie.equals(".")) {
            DialogueUtil.afficherAvertissement("Saisie vide", "Saisissez une quantite avant de l'appliquer.");
            return;
        }
        if (panier.isEmpty()) {
            DialogueUtil.afficherAvertissement("Panier vide", "Ajoutez d'abord un produit.");
            effacerSaisie();
            return;
        }

        // On accepte les quantites decimales (ex: 1.5 kg) via BigDecimal,
        // puis on verifie quand meme que c'est > 0.
        BigDecimal quantiteSaisie;
        try {
            quantiteSaisie = new BigDecimal(saisie);
        } catch (NumberFormatException e) {
            DialogueUtil.afficherAvertissement("Quantite invalide", "Saisissez un nombre valide (ex: 2 ou 1.5).");
            return;
        }
        if (quantiteSaisie.compareTo(BigDecimal.ZERO) <= 0) {
            DialogueUtil.afficherAvertissement("Quantite invalide", "La quantite doit etre superieure a zero.");
            return;
        }

        LignePanier derniereLigne = panier.get(panier.size() - 1);
        int idDepot = ContexteApplication.getInstance().getIdDepotCourant();
        int stockDisponible = ContexteApplication.getInstance().getStockService()
                .lireQuantite(derniereLigne.getProduit().getId(), idDepot);

        // Pour la verification du stock on compare en entier (arrondi superieur)
        // afin de ne pas vendre plus que disponible meme pour les produits au poids.
        if (quantiteSaisie.setScale(0, java.math.RoundingMode.CEILING).intValue() > stockDisponible) {
            DialogueUtil.afficherAvertissement("Stock insuffisant",
                    "Stock disponible : " + stockDisponible);
            return;
        }

        // LignePanier stocke un int pour la quantite (produits a l'unite) ;
        // pour les produits au poids on arrondit au superieur.
        derniereLigne.setQuantite(quantiteSaisie.setScale(0, java.math.RoundingMode.CEILING).intValue());
        tablePanier.refresh();
        rafraichirTotal();
        effacerSaisie();
    }

    // ------------------------------------------------------------------
    // Panier (commun aux deux modes)
    // ------------------------------------------------------------------

    private void ajouterProduitAuPanier(Produit produit, int quantiteAjoutee) {
        int idDepot = ContexteApplication.getInstance().getIdDepotCourant();
        int stockDisponible = ContexteApplication.getInstance().getStockService()
                .lireQuantite(produit.getId(), idDepot);

        Optional<LignePanier> ligneExistante = panier.stream()
                .filter(l -> l.getProduit().getId().equals(produit.getId()))
                .findFirst();

        int dejaDansPanier = ligneExistante.map(LignePanier::getQuantite).orElse(0);
        if (dejaDansPanier + quantiteAjoutee > stockDisponible) {
            DialogueUtil.afficherAvertissement("Stock insuffisant",
                    "Stock disponible pour " + produit.getDesignation() + " : " + stockDisponible);
            return;
        }
        if (ligneExistante.isPresent()) {
            ligneExistante.get().setQuantite(dejaDansPanier + quantiteAjoutee);
            tablePanier.refresh();
        } else {
            panier.add(new LignePanier(produit, quantiteAjoutee));
        }
        Toolkit.getDefaultToolkit().beep();
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
        if (!panier.isEmpty() &&
                !DialogueUtil.confirmer("Annuler le ticket", "Voulez-vous vider le panier en cours ?")) {
            return;
        }
        panier.clear();
        champCarteFidelite.clear();
        if (labelDernierProduitAjoute != null) {
            labelDernierProduitAjoute.setText("Dernier article : (aucun)");
        }
        rafraichirTotal();
    }

    private void rafraichirTotal() {
        BigDecimal total = panier.stream().map(LignePanier::getTotalLigne)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        labelTotalTicket.setText(com.supermarche.util.FormatUtil.montant(total));
    }

    @FXML
    private void validerVente() {
        if (sessionOuverte == null) {
            DialogueUtil.afficherAvertissement("Session requise",
                    "Ouvrez une session de caisse avant d'enregistrer une vente.");
            return;
        }
        if (panier.isEmpty()) {
            DialogueUtil.afficherAvertissement("Panier vide",
                    "Ajoutez au moins un produit avant de valider.");
            return;
        }
        BigDecimal montantPaye;
        try {
            String saisie = champMontantPaiement.getText();
            BigDecimal total = panier.stream().map(LignePanier::getTotalLigne)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            montantPaye = (saisie == null || saisie.isBlank())
                    ? total : new BigDecimal(saisie.replace(",", "."));
        } catch (NumberFormatException e) {
            DialogueUtil.afficherAvertissement("Montant invalide",
                    "Saisissez un montant de paiement numerique.");
            return;
        }
        Integer idClient = null;
        String carte = champCarteFidelite.getText();
        if (carte != null && !carte.isBlank()) {
            Optional<Client> client = ContexteApplication.getInstance()
                    .getClientService().trouverParCarteFidelite(carte.trim());
            if (client.isPresent()) {
                idClient = client.get().getId();
            } else {
                DialogueUtil.afficherAvertissement("Carte inconnue",
                        "Aucun client ne correspond a cette carte. La vente continuera sans client.");
            }
        }
        List<LigneVente> lignes = new ArrayList<>();
        for (LignePanier lp : panier) {
            Produit p = lp.getProduit();
            lignes.add(new LigneVente(p.getId(), p.getDesignation(),
                    BigDecimal.valueOf(lp.getQuantite()), p.getPrixVenteTtc(), p.getTauxTva()));
        }
        List<Paiement> paiements = List.of(new Paiement(comboModePaiement.getValue(), montantPaye));
        try {
            Vente vente = ContexteApplication.getInstance().getVenteService().enregistrerVente(
                    sessionOuverte.getId(), ContexteApplication.getInstance().getIdDepotCourant(),
                    idClient, lignes, paiements);
            BigDecimal monnaie = montantPaye.subtract(vente.getTotalTtc());
            String msgMonnaie = monnaie.compareTo(BigDecimal.ZERO) > 0
                    ? "\nMonnaie a rendre : " + com.supermarche.util.FormatUtil.montant(monnaie) : "";
            DialogueUtil.afficherInfo("Vente enregistree",
                    "Ticket " + vente.getNumeroTicket()
                    + "\nTotal : " + com.supermarche.util.FormatUtil.montant(vente.getTotalTtc())
                    + msgMonnaie);
            panier.clear();
            champCarteFidelite.clear();
            champMontantPaiement.clear();
            if (labelDernierProduitAjoute != null) {
                labelDernierProduitAjoute.setText("Dernier article : (aucun)");
            }
            rafraichirTotal();
        } catch (SupermarcheException e) {
            DialogueUtil.afficherErreur("Vente impossible", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Session de caisse
    // ------------------------------------------------------------------

    @FXML
    private void ouvrirSession() {
        Caisse caisse = comboCaisse.getValue();
        if (caisse == null) {
            DialogueUtil.afficherAvertissement("Caisse requise",
                    "Selectionnez une caisse avant d'ouvrir une session.");
            return;
        }
        BigDecimal fond;
        try {
            fond = new BigDecimal(champFondOuverture.getText().replace(",", "."));
        } catch (Exception e) {
            DialogueUtil.afficherAvertissement("Montant invalide",
                    "Saisissez un montant numerique pour le fond de caisse.");
            return;
        }
        try {
            sessionOuverte = ContexteApplication.getInstance().getCaisseService()
                    .ouvrirSession(caisse.getId(), fond);
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
            String msgEcart = fermee.getEcart().compareTo(BigDecimal.ZERO) == 0
                    ? "Aucun ecart constate."
                    : "Ecart : " + com.supermarche.util.FormatUtil.montant(fermee.getEcart())
                    + (fermee.getEcart().compareTo(BigDecimal.ZERO) > 0 ? " (excedent)" : " (manquant)");
            DialogueUtil.afficherInfo("Session fermee",
                    "Fond theorique : " + com.supermarche.util.FormatUtil.montant(fermee.getFondFermetureTheorique())
                    + "\nFond reel : " + com.supermarche.util.FormatUtil.montant(fondReel)
                    + "\n" + msgEcart);
            sessionOuverte = null;
            ContexteApplication.getInstance().reinitialiserSession();
            rafraichirEtatBoutonsSession();
        } catch (NumberFormatException e) {
            DialogueUtil.afficherAvertissement("Montant invalide", "Saisissez un montant numerique.");
        } catch (SupermarcheException e) {
            DialogueUtil.afficherErreur("Fermeture impossible", e.getMessage());
        }
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
        // Bouton verrouiller visible seulement si session active
        boutonVerrouiller.setVisible(sessionActive);
        boutonVerrouiller.setManaged(sessionActive);
        // Demarrer/arreter la minuterie
        if (sessionActive && minuterieVerrouillage != null) {
            minuterieVerrouillage.playFromStart();
        } else if (minuterieVerrouillage != null) {
            minuterieVerrouillage.stop();
        }
    }

    private void chargerCaissesDisponibles() {
        List<Caisse> caisses = ContexteApplication.getInstance()
                .getCaisseService().listerCaissesActives();
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
        Optional<SessionCaisse> session = ContexteApplication.getInstance()
                .getCaisseService().trouverSessionOuverte(caisse.getId());
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

    private void configurerColonnes() {
        colPanierDesignation.setCellValueFactory(
                c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDesignation()));
        colPanierQuantite.setCellValueFactory(
                c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getQuantite()));
        colPanierPrixUnitaire.setCellValueFactory(
                c -> new javafx.beans.property.SimpleStringProperty(
                        com.supermarche.util.FormatUtil.montant(c.getValue().getPrixUnitaire())));
        colPanierTotal.setCellValueFactory(
                c -> new javafx.beans.property.SimpleStringProperty(
                        com.supermarche.util.FormatUtil.montant(c.getValue().getTotalLigne())));
    }

    @FXML
    private void seDeconnecter() {
        boolean confirme = DialogueUtil.confirmer("Deconnexion", "Voulez-vous vraiment vous deconnecter ?");
        if (!confirme) {
            return;
        }
        ContexteApplication.getInstance().getAuthService().seDeconnecter();
        ContexteApplication.getInstance().reinitialiserSession();
        try {
            com.supermarche.MainApp.afficherEcranConnexion();
        } catch (IOException e) {
            DialogueUtil.afficherErreur("Erreur", "Impossible de revenir a l'ecran de connexion.");
        }
    }

    @FXML
    public void verrouillerCaisse() {
        if (sessionOuverte == null) return;
        minuterieVerrouillage.stop();
        champMotDePasseVerrou.clear();
        labelErreurVerrou.setVisible(false);
        labelErreurVerrou.setManaged(false);
        var u = ContexteApplication.getInstance()
                .getAuthService().getUtilisateurConnecte();
        labelNomCaissierVerrou.setText(
                u != null ? u.getEmploye().getNomComplet() : "");
        panneauVerrouillage.setVisible(true);
        panneauVerrouillage.setManaged(true);
        champMotDePasseVerrou.requestFocus();
    }

    @FXML
    private void deverrouillerCaisse() {
        String mdp = champMotDePasseVerrou.getText();
        if (mdp == null || mdp.isBlank()) {
            afficherErreurVerrou("Saisissez votre mot de passe.");
            return;
        }
        var u = ContexteApplication.getInstance()
                .getAuthService().getUtilisateurConnecte();
        if (u == null) {
            afficherErreurVerrou("Aucun utilisateur connecte.");
            return;
        }
        if (!PasswordHasher.verifier(mdp, u.getMotDePasseHash())) {
            afficherErreurVerrou("Mot de passe incorrect.");
            champMotDePasseVerrou.clear();
            champMotDePasseVerrou.requestFocus();
            return;
        }
        panneauVerrouillage.setVisible(false);
        panneauVerrouillage.setManaged(false);
        reinitialiserMinuterie();
    }

    private void afficherErreurVerrou(String message) {
        labelErreurVerrou.setText(message);
        labelErreurVerrou.setVisible(true);
        labelErreurVerrou.setManaged(true);
    }

    private void reinitialiserMinuterie() {
        if (sessionOuverte != null && !panneauVerrouillage.isVisible()) {
            minuterieVerrouillage.playFromStart();
        }
    }
}