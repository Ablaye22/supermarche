-- ============================================================================
-- Systeme de Gestion de Supermarche - Schema MariaDB
-- ============================================================================
-- Moteur : InnoDB (transactions + cles etrangeres)
-- Charset : utf8mb4 (support complet Unicode, emojis, accents)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS supermarche
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE supermarche;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- MODULE SECURITE : roles, utilisateurs, journalisation
-- ============================================================================

CREATE TABLE roles (
    id_role         INT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(30)  NOT NULL UNIQUE,   -- ADMIN, GERANT, CAISSIER, MAGASINIER
    libelle         VARCHAR(100) NOT NULL,
    description     TEXT
) ENGINE=InnoDB;

CREATE TABLE permissions (
    id_permission   INT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(60) NOT NULL UNIQUE,    -- ex: PRODUIT_CREER, VENTE_ANNULER, RAPPORT_VOIR
    description     VARCHAR(200)
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
    id_role         INT NOT NULL,
    id_permission   INT NOT NULL,
    PRIMARY KEY (id_role, id_permission),
    FOREIGN KEY (id_role) REFERENCES roles(id_role) ON DELETE CASCADE,
    FOREIGN KEY (id_permission) REFERENCES permissions(id_permission) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE employes (
    id_employe      INT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(80)  NOT NULL,
    prenom          VARCHAR(80)  NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    telephone       VARCHAR(30),
    date_embauche   DATE,
    actif           BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Comptes de connexion (separes de l'identite employe : un employe peut
-- exister sans compte, et on isole les donnees d'authentification)
CREATE TABLE utilisateurs (
    id_utilisateur      INT AUTO_INCREMENT PRIMARY KEY,
    id_employe          INT NOT NULL UNIQUE,
    nom_utilisateur     VARCHAR(50) NOT NULL UNIQUE,
    mot_de_passe_hash   VARCHAR(255) NOT NULL,        -- BCrypt
    id_role             INT NOT NULL,
    actif               BOOLEAN NOT NULL DEFAULT TRUE,
    compte_verrouille   BOOLEAN NOT NULL DEFAULT FALSE,
    tentatives_echouees INT NOT NULL DEFAULT 0,
    derniere_connexion  DATETIME NULL,
    date_creation       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_employe) REFERENCES employes(id_employe) ON DELETE CASCADE,
    FOREIGN KEY (id_role) REFERENCES roles(id_role)
) ENGINE=InnoDB;

-- Journal d'audit : trace toute action sensible (connexion, vente, modif stock...)
CREATE TABLE journal_audit (
    id_journal      BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_utilisateur  INT NULL,
    action          VARCHAR(60)  NOT NULL,   -- CONNEXION, VENTE_CREEE, PRODUIT_MODIFIE, ...
    entite          VARCHAR(60)  NULL,       -- nom de la table/entite concernee
    id_entite       BIGINT NULL,             -- id de l'enregistrement concerne
    details         TEXT NULL,               -- JSON ou texte libre
    adresse_ip      VARCHAR(45) NULL,
    horodatage      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur) ON DELETE SET NULL,
    INDEX idx_journal_horodatage (horodatage),
    INDEX idx_journal_utilisateur (id_utilisateur)
) ENGINE=InnoDB;

-- ============================================================================
-- MODULE PRODUITS / STOCKS / FOURNISSEURS
-- ============================================================================

CREATE TABLE categories (
    id_categorie    INT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(100) NOT NULL UNIQUE,
    id_categorie_parente INT NULL,
    FOREIGN KEY (id_categorie_parente) REFERENCES categories(id_categorie) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE fournisseurs (
    id_fournisseur  INT AUTO_INCREMENT PRIMARY KEY,
    raison_sociale  VARCHAR(150) NOT NULL,
    siret           VARCHAR(20),
    contact_nom     VARCHAR(100),
    telephone       VARCHAR(30),
    email           VARCHAR(150),
    adresse         VARCHAR(255),
    actif           BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE produits (
    id_produit          INT AUTO_INCREMENT PRIMARY KEY,
    code_barre          VARCHAR(50) NOT NULL UNIQUE,
    designation         VARCHAR(150) NOT NULL,
    description         TEXT,
    id_categorie        INT NULL,
    id_fournisseur_principal INT NULL,
    prix_achat_ht       DECIMAL(10,2) NOT NULL DEFAULT 0,
    prix_vente_ttc      DECIMAL(10,2) NOT NULL DEFAULT 0,
    taux_tva            DECIMAL(5,2)  NOT NULL DEFAULT 20.00,
    unite               VARCHAR(20)   NOT NULL DEFAULT 'unite', -- unite, kg, litre...
    seuil_alerte_stock  INT NOT NULL DEFAULT 5,
    actif               BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    plus                VARCHAR(5),
    FOREIGN KEY (id_categorie) REFERENCES categories(id_categorie) ON DELETE SET NULL,
    FOREIGN KEY (id_fournisseur_principal) REFERENCES fournisseurs(id_fournisseur) ON DELETE SET NULL,
    INDEX idx_produit_designation (designation)
) ENGINE=InnoDB;

-- Stock par depot/rayon (extensible a plusieurs entrepots/magasins plus tard)
CREATE TABLE depots (
    id_depot        INT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(100) NOT NULL UNIQUE,
    adresse         VARCHAR(255),
    est_point_vente BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE stocks (
    id_produit      INT NOT NULL,
    id_depot        INT NOT NULL,
    quantite        INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id_produit, id_depot),
    FOREIGN KEY (id_produit) REFERENCES produits(id_produit) ON DELETE CASCADE,
    FOREIGN KEY (id_depot) REFERENCES depots(id_depot) ON DELETE CASCADE,
    CONSTRAINT chk_quantite_non_negative CHECK (quantite >= 0)
) ENGINE=InnoDB;

-- Historique de tous les mouvements de stock (entree, sortie, ajustement, vente)
CREATE TABLE mouvements_stock (
    id_mouvement    BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_produit      INT NOT NULL,
    id_depot        INT NOT NULL,
    type_mouvement  ENUM('ENTREE','SORTIE','VENTE','AJUSTEMENT','RETOUR') NOT NULL,
    quantite        INT NOT NULL,             -- toujours positif, le type donne le sens
    quantite_apres  INT NOT NULL,             -- quantite en stock apres le mouvement (tracabilite)
    reference       VARCHAR(60),              -- ex: numero de vente, numero de commande fournisseur
    id_utilisateur  INT NULL,
    commentaire     VARCHAR(255),
    horodatage      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_produit) REFERENCES produits(id_produit),
    FOREIGN KEY (id_depot) REFERENCES depots(id_depot),
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur) ON DELETE SET NULL,
    INDEX idx_mouvement_produit (id_produit),
    INDEX idx_mouvement_horodatage (horodatage)
) ENGINE=InnoDB;

CREATE TABLE commandes_fournisseur (
    id_commande         INT AUTO_INCREMENT PRIMARY KEY,
    id_fournisseur       INT NOT NULL,
    statut               ENUM('BROUILLON','ENVOYEE','RECUE_PARTIELLE','RECUE','ANNULEE') NOT NULL DEFAULT 'BROUILLON',
    date_commande         DATE NOT NULL,
    date_reception_prevue DATE NULL,
    id_utilisateur        INT NULL,
    date_creation          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_fournisseur) REFERENCES fournisseurs(id_fournisseur),
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE lignes_commande_fournisseur (
    id_ligne            BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_commande          INT NOT NULL,
    id_produit           INT NOT NULL,
    quantite_commandee   INT NOT NULL,
    quantite_recue       INT NOT NULL DEFAULT 0,
    prix_achat_unitaire  DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_commande) REFERENCES commandes_fournisseur(id_commande) ON DELETE CASCADE,
    FOREIGN KEY (id_produit) REFERENCES produits(id_produit)
) ENGINE=InnoDB;

-- ============================================================================
-- MODULE CLIENTS
-- ============================================================================

CREATE TABLE clients (
    id_client       INT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(80) NOT NULL,
    prenom          VARCHAR(80),
    email           VARCHAR(150),
    telephone       VARCHAR(30),
    carte_fidelite  VARCHAR(30) UNIQUE,
    points_fidelite INT NOT NULL DEFAULT 0,
    date_creation   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================================
-- MODULE CAISSE / VENTES
-- ============================================================================

CREATE TABLE caisses (
    id_caisse       INT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(50) NOT NULL UNIQUE,   -- ex: "Caisse 1"
    id_depot        INT NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (id_depot) REFERENCES depots(id_depot)
) ENGINE=InnoDB;

-- Une session de caisse = une ouverture/fermeture par un caissier
-- (fond de caisse, controle des ecarts)
CREATE TABLE sessions_caisse (
    id_session          BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_caisse            INT NOT NULL,
    id_utilisateur        INT NOT NULL,
    fond_ouverture        DECIMAL(10,2) NOT NULL,
    fond_fermeture_theorique DECIMAL(10,2) NULL,
    fond_fermeture_reel   DECIMAL(10,2) NULL,
    ecart                 DECIMAL(10,2) NULL,
    date_ouverture        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_fermeture        DATETIME NULL,
    statut                ENUM('OUVERTE','FERMEE') NOT NULL DEFAULT 'OUVERTE',
    FOREIGN KEY (id_caisse) REFERENCES caisses(id_caisse),
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur)
) ENGINE=InnoDB;

CREATE TABLE ventes (
    id_vente            BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_ticket        VARCHAR(30) NOT NULL UNIQUE,
    id_session            BIGINT NOT NULL,
    id_client             INT NULL,
    total_ht              DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_tva              DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_ttc              DECIMAL(10,2) NOT NULL DEFAULT 0,
    statut                 ENUM('EN_COURS','VALIDEE','ANNULEE','REMBOURSEE') NOT NULL DEFAULT 'EN_COURS',
    horodatage              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_session) REFERENCES sessions_caisse(id_session),
    FOREIGN KEY (id_client) REFERENCES clients(id_client) ON DELETE SET NULL,
    INDEX idx_vente_horodatage (horodatage)
) ENGINE=InnoDB;

CREATE TABLE lignes_vente (
    id_ligne_vente      BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_vente             BIGINT NOT NULL,
    id_produit           INT NOT NULL,
    quantite              DECIMAL(10,3) NOT NULL,   -- decimal pour produits au poids
    prix_unitaire_ttc     DECIMAL(10,2) NOT NULL,    -- prix au moment de la vente (historise)
    taux_tva              DECIMAL(5,2) NOT NULL,
    remise_pourcentage    DECIMAL(5,2) NOT NULL DEFAULT 0,
    total_ligne_ttc       DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_vente) REFERENCES ventes(id_vente) ON DELETE CASCADE,
    FOREIGN KEY (id_produit) REFERENCES produits(id_produit)
) ENGINE=InnoDB;

CREATE TABLE paiements (
    id_paiement     BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_vente         BIGINT NOT NULL,
    mode             ENUM('ESPECES','CARTE','CHEQUE','TICKET_RESTO','AVOIR') NOT NULL,
    montant          DECIMAL(10,2) NOT NULL,
    horodatage       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_vente) REFERENCES ventes(id_vente) ON DELETE CASCADE
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE planning (
    id_planning INT AUTO_INCREMENT PRIMARY KEY,
    id_employe INT NOT NULL,
    date_travail DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    pause_minutes INT DEFAULT 0,
    poste VARCHAR(100),
    statut ENUM('Prévu','Présent','Absent','Congé') DEFAULT 'Prévu',

    FOREIGN KEY(id_employe)
        REFERENCES employes(id_employe)
);
