# Modèle de données (entité-relation)

Reflet fidèle du schéma SQL défini dans `database/01_schema.sql`.

```mermaid
erDiagram
    ROLES ||--o{ ROLE_PERMISSIONS : possede
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : accordee_a
    EMPLOYES ||--o| UTILISATEURS : a_un_compte
    ROLES ||--o{ UTILISATEURS : attribue_a
    UTILISATEURS ||--o{ JOURNAL_AUDIT : effectue

    CATEGORIES ||--o{ PRODUITS : classe
    FOURNISSEURS ||--o{ PRODUITS : fournit_principalement
    PRODUITS ||--o{ STOCKS : a_un_stock
    DEPOTS ||--o{ STOCKS : contient
    PRODUITS ||--o{ MOUVEMENTS_STOCK : concerne
    DEPOTS ||--o{ MOUVEMENTS_STOCK : localise
    UTILISATEURS ||--o{ MOUVEMENTS_STOCK : effectue_par

    FOURNISSEURS ||--o{ COMMANDES_FOURNISSEUR : recoit
    COMMANDES_FOURNISSEUR ||--o{ LIGNES_COMMANDE_FOURNISSEUR : contient
    PRODUITS ||--o{ LIGNES_COMMANDE_FOURNISSEUR : commande

    DEPOTS ||--o{ CAISSES : heberge
    CAISSES ||--o{ SESSIONS_CAISSE : ouverte_sur
    UTILISATEURS ||--o{ SESSIONS_CAISSE : ouvre

    SESSIONS_CAISSE ||--o{ VENTES : enregistre
    CLIENTS ||--o{ VENTES : pour
    VENTES ||--o{ LIGNES_VENTE : contient
    PRODUITS ||--o{ LIGNES_VENTE : vendu
    VENTES ||--o{ PAIEMENTS : regle_par

    ROLES {
        int id_role PK
        varchar code
        varchar libelle
    }
    PERMISSIONS {
        int id_permission PK
        varchar code
    }
    EMPLOYES {
        int id_employe PK
        varchar nom
        varchar prenom
        varchar email
        boolean actif
    }
    UTILISATEURS {
        int id_utilisateur PK
        int id_employe FK
        varchar nom_utilisateur
        varchar mot_de_passe_hash
        int id_role FK
        boolean compte_verrouille
        int tentatives_echouees
    }
    JOURNAL_AUDIT {
        bigint id_journal PK
        int id_utilisateur FK
        varchar action
        varchar entite
        bigint id_entite
        datetime horodatage
    }
    CATEGORIES {
        int id_categorie PK
        varchar nom
    }
    FOURNISSEURS {
        int id_fournisseur PK
        varchar raison_sociale
        varchar siret
    }
    PRODUITS {
        int id_produit PK
        varchar code_barre
        varchar designation
        decimal prix_achat_ht
        decimal prix_vente_ttc
        decimal taux_tva
        int seuil_alerte_stock
    }
    DEPOTS {
        int id_depot PK
        varchar nom
        boolean est_point_vente
    }
    STOCKS {
        int id_produit PK_FK
        int id_depot PK_FK
        int quantite
    }
    MOUVEMENTS_STOCK {
        bigint id_mouvement PK
        int id_produit FK
        int id_depot FK
        enum type_mouvement
        int quantite
        int quantite_apres
    }
    COMMANDES_FOURNISSEUR {
        bigint id_commande PK
        int id_fournisseur FK
        enum statut
        date date_commande
    }
    LIGNES_COMMANDE_FOURNISSEUR {
        bigint id_ligne PK
        bigint id_commande FK
        int id_produit FK
        int quantite_commandee
        int quantite_recue
    }
    CLIENTS {
        int id_client PK
        varchar nom
        varchar carte_fidelite
        int points_fidelite
    }
    CAISSES {
        int id_caisse PK
        varchar nom
        int id_depot FK
        boolean active
    }
    SESSIONS_CAISSE {
        bigint id_session PK
        int id_caisse FK
        int id_utilisateur FK
        decimal fond_ouverture
        decimal ecart
        enum statut
    }
    VENTES {
        bigint id_vente PK
        varchar numero_ticket
        bigint id_session FK
        int id_client FK
        decimal total_ttc
        enum statut
    }
    LIGNES_VENTE {
        bigint id_ligne_vente PK
        bigint id_vente FK
        int id_produit FK
        decimal quantite
        decimal prix_unitaire_ttc
        decimal total_ligne_ttc
    }
    PAIEMENTS {
        bigint id_paiement PK
        bigint id_vente FK
        enum mode
        decimal montant
    }
```

## Choix de modélisation notables

- **`stocks` a une clé primaire composite** `(id_produit, id_depot)` : un produit a une quantité distincte par dépôt, ce qui anticipe une extension multi-magasins sans changer le schéma.
- **`lignes_vente` historise `prix_unitaire_ttc` et `taux_tva`** plutôt que de les recalculer depuis `produits` : un ticket de caisse doit rester fidèle au prix payé même si le tarif catalogue change ensuite.
- **`mouvements_stock` conserve `quantite_apres`** (la quantité résultante, pas seulement le delta) : permet de retrouver l'état du stock à un instant donné sans avoir à rejouer tout l'historique.
- **Suppression douce uniquement** : les entités `produits`, `fournisseurs`, `employes`, `utilisateurs` ont un champ `actif` plutôt qu'une suppression physique, pour préserver l'intégrité référentielle avec l'historique des ventes/commandes passées.
