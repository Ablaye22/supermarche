# Diagramme de classes — Modèle de domaine

Ce diagramme représente les entités métier principales et leurs relations.
Les classes techniques (DAO, services, contrôleurs JavaFX) ne sont pas représentées ici ; voir `architecture.md` pour la vue en couches.

```mermaid
classDiagram
    class Employe {
        -Integer id
        -String nom
        -String prenom
        -String email
        -String telephone
        -LocalDate dateEmbauche
        -boolean actif
        +getNomComplet() String
    }

    class Utilisateur {
        -Integer id
        -String nomUtilisateur
        -String motDePasseHash
        -boolean actif
        -boolean compteVerrouille
        -int tentativesEchouees
        -LocalDateTime derniereConnexion
        +possedePermission(String) boolean
    }

    class Role {
        -Integer id
        -CodeRole code
        -String libelle
        -Set~String~ permissions
        +possedePermission(String) boolean
    }

    class CodeRole {
        <<enumeration>>
        ADMIN
        GERANT
        CAISSIER
        MAGASINIER
    }

    class Produit {
        -Integer id
        -String codeBarre
        -String designation
        -BigDecimal prixAchatHt
        -BigDecimal prixVenteTtc
        -BigDecimal tauxTva
        -String unite
        -int seuilAlerteStock
        -boolean actif
        +getMargeBrute() BigDecimal
    }

    class Categorie {
        -Integer id
        -String nom
        -Integer idCategorieParente
    }

    class Fournisseur {
        -Integer id
        -String raisonSociale
        -String siret
        -String contactNom
        -boolean actif
    }

    class Depot {
        -Integer id
        -String nom
        -boolean pointDeVente
    }

    class Stock {
        -Integer idProduit
        -Integer idDepot
        -int quantite
    }

    class MouvementStock {
        -Long id
        -TypeMouvement typeMouvement
        -int quantite
        -int quantiteApres
        -String reference
        -LocalDateTime horodatage
    }

    class TypeMouvement {
        <<enumeration>>
        ENTREE
        SORTIE
        VENTE
        AJUSTEMENT
        RETOUR
    }

    class CommandeFournisseur {
        -Long id
        -StatutCommande statut
        -LocalDate dateCommande
        -LocalDate dateReceptionPrevue
        +ajouterLigne(LigneCommandeFournisseur)
    }

    class LigneCommandeFournisseur {
        -Long id
        -int quantiteCommandee
        -int quantiteRecue
        -BigDecimal prixAchatUnitaire
        +estEntierementRecue() boolean
        +getQuantiteRestante() int
    }

    class Client {
        -Integer id
        -String nom
        -String prenom
        -String carteFidelite
        -int pointsFidelite
        +getNomComplet() String
    }

    class Caisse {
        -Integer id
        -String nom
        -Integer idDepot
        -boolean active
    }

    class SessionCaisse {
        -Long id
        -BigDecimal fondOuverture
        -BigDecimal fondFermetureTheorique
        -BigDecimal fondFermetureReel
        -BigDecimal ecart
        -StatutSession statut
    }

    class Vente {
        -Long id
        -String numeroTicket
        -BigDecimal totalHt
        -BigDecimal totalTva
        -BigDecimal totalTtc
        -StatutVente statut
        +ajouterLigne(LigneVente)
        +getTotalPaye() BigDecimal
        +getResteAPayer() BigDecimal
    }

    class LigneVente {
        -Long id
        -BigDecimal quantite
        -BigDecimal prixUnitaireTtc
        -BigDecimal tauxTva
        -BigDecimal remisePourcentage
        -BigDecimal totalLigneTtc
        +recalculerTotal()
    }

    class Paiement {
        -Long id
        -ModePaiement mode
        -BigDecimal montant
    }

    class ModePaiement {
        <<enumeration>>
        ESPECES
        CARTE
        CHEQUE
        TICKET_RESTO
        AVOIR
    }

    class EntreeJournalAudit {
        -Long id
        -String action
        -String entite
        -Long idEntite
        -String details
        -LocalDateTime horodatage
    }

    %% ---- Relations Sécurité ----
    Employe "1" -- "0..1" Utilisateur : possède un compte
    Utilisateur "*" --> "1" Role : a un rôle
    Role "1" o-- "*" CodeRole : référence

    %% ---- Relations Produits / Stock / Fournisseurs ----
    Produit "*" --> "0..1" Categorie : appartient à
    Produit "*" --> "0..1" Fournisseur : fourni par
    Stock "*" --> "1" Produit
    Stock "*" --> "1" Depot
    MouvementStock "*" --> "1" Produit
    MouvementStock "*" --> "1" Depot
    CommandeFournisseur "1" *-- "*" LigneCommandeFournisseur : contient
    CommandeFournisseur "*" --> "1" Fournisseur : adressée à
    LigneCommandeFournisseur "*" --> "1" Produit : porte sur

    %% ---- Relations Caisse / Vente ----
    Caisse "*" --> "1" Depot : rattachée à
    SessionCaisse "*" --> "1" Caisse : ouverte sur
    SessionCaisse "*" --> "1" Utilisateur : ouverte par
    Vente "*" --> "1" SessionCaisse : enregistrée dans
    Vente "*" --> "0..1" Client : pour
    Vente "1" *-- "*" LigneVente : contient
    Vente "1" *-- "*" Paiement : réglée par
    LigneVente "*" --> "1" Produit : porte sur

    %% ---- Audit ----
    EntreeJournalAudit "*" --> "0..1" Utilisateur : effectuée par
```

## Notes de lecture

- **Employe / Utilisateur** : séparation volontaire entre l'identité (Employe) et le compte de connexion (Utilisateur) — un employé peut exister sans avoir d'accès informatique.
- **Role / permissions** : les permissions sont gérées comme un ensemble de codes (`String`) plutôt que des classes dédiées, pour rester simples à étendre côté base de données sans migration de schéma Java.
- **LigneVente** historise `prixUnitaireTtc` et `tauxTva` au moment de la vente : un changement de prix catalogue ultérieur n'affecte jamais un ticket déjà émis.
- **MouvementStock** trace tout changement de stock avec la quantité résultante (`quantiteApres`), ce qui permet de reconstituer l'historique complet d'un produit sans recalcul.
