-- ============================================================================
-- Donnees initiales (seed) - a executer une seule fois apres 01_schema.sql
-- ============================================================================

USE supermarche;

-- ---------------------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------------------
INSERT INTO roles (code, libelle, description) VALUES
    ('ADMIN',     'Administrateur', 'Acces complet : configuration, utilisateurs, securite'),
    ('GERANT',    'Gerant',         'Gestion produits, stocks, fournisseurs, rapports'),
    ('CAISSIER',  'Caissier',       'Encaissement, ouverture/fermeture de caisse'),
    ('MAGASINIER','Magasinier',     'Gestion des stocks et reception des commandes');

-- ---------------------------------------------------------------------------
-- Permissions (granulaires, assignees aux roles ci-dessous)
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, description) VALUES
    ('UTILISATEUR_GERER',   'Creer/modifier/desactiver des comptes utilisateurs'),
    ('PRODUIT_GERER',       'Creer/modifier/desactiver des produits'),
    ('STOCK_GERER',         'Effectuer des entrees/sorties/ajustements de stock'),
    ('FOURNISSEUR_GERER',   'Creer/modifier des fournisseurs et commandes'),
    ('CLIENT_GERER',        'Creer/modifier des fiches clients'),
    ('VENTE_CREER',         'Enregistrer une vente en caisse'),
    ('VENTE_ANNULER',       'Annuler ou rembourser une vente'),
    ('CAISSE_OUVRIR_FERMER','Ouvrir et fermer une session de caisse'),
    ('RAPPORT_VOIR',        'Consulter les rapports et statistiques'),
    ('JOURNAL_VOIR',        'Consulter le journal d''audit');

-- ---------------------------------------------------------------------------
-- Attribution des permissions par role
-- ---------------------------------------------------------------------------

-- ADMIN : toutes les permissions
INSERT INTO role_permissions (id_role, id_permission)
SELECT (SELECT id_role FROM roles WHERE code = 'ADMIN'), id_permission FROM permissions;

-- GERANT : tout sauf gestion des utilisateurs
INSERT INTO role_permissions (id_role, id_permission)
SELECT (SELECT id_role FROM roles WHERE code = 'GERANT'), id_permission
FROM permissions WHERE code != 'UTILISATEUR_GERER';

-- CAISSIER : vente + caisse uniquement
INSERT INTO role_permissions (id_role, id_permission)
SELECT (SELECT id_role FROM roles WHERE code = 'CAISSIER'), id_permission
FROM permissions WHERE code IN ('VENTE_CREER', 'CAISSE_OUVRIR_FERMER', 'CLIENT_GERER');

-- MAGASINIER : stock + fournisseurs
INSERT INTO role_permissions (id_role, id_permission)
SELECT (SELECT id_role FROM roles WHERE code = 'MAGASINIER'), id_permission
FROM permissions WHERE code IN ('STOCK_GERER', 'FOURNISSEUR_GERER', 'PRODUIT_GERER');

-- ---------------------------------------------------------------------------
-- Depot et caisse par defaut
-- ---------------------------------------------------------------------------
INSERT INTO depots (nom, adresse, est_point_vente) VALUES
    ('Magasin principal', 'A definir', TRUE);

INSERT INTO caisses (nom, id_depot, active) VALUES
    ('Caisse 1', (SELECT id_depot FROM depots WHERE nom = 'Magasin principal'), TRUE),
    ('Caisse 2', (SELECT id_depot FROM depots WHERE nom = 'Magasin principal'), TRUE);

-- ---------------------------------------------------------------------------
-- Compte administrateur initial
--
-- ATTENTION : ce script NE CONTIENT PAS de mot de passe pre-rempli.
-- Le hash BCrypt doit etre genere localement chez vous (jamais transmis
-- en clair ni invente), puis colle ci-dessous avant d'executer ce fichier :
--
--   1. Compilez le projet : mvn compile
--   2. Generez un hash avec votre propre mot de passe :
--        mvn exec:java -Dexec.mainClass="com.supermarche.security.PasswordHasher" -Dexec.args="VotreMotDePasseSolide"
--   3. Copiez le hash affiche (commence par $2a$ ou $2b$) dans la ligne
--      INSERT ci-dessous, a la place de '<HASH_BCRYPT_A_GENERER>'.
--   4. Choisissez un mot de passe fort, unique, et changez-le a la
--      premiere connexion.
-- ---------------------------------------------------------------------------
INSERT INTO employes (nom, prenom, email, date_embauche, actif) VALUES
    ('Admin', 'Systeme', 'admin@supermarche.local', CURDATE(), TRUE);

INSERT INTO utilisateurs (id_employe, nom_utilisateur, mot_de_passe_hash, id_role, actif) VALUES
    (
        (SELECT id_employe FROM employes WHERE email = 'admin@supermarche.local'),
        'admin',
        '$2a$12$4Vy2rKgcEv1Qj6qkBRNzsugtmRBZj8vfqYEVUznUVVqNvsaGa1woe',
        (SELECT id_role FROM roles WHERE code = 'ADMIN'),
        TRUE
    );
