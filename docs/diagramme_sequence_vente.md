# Diagramme de séquence — Enregistrement d'une vente

Ce flux est le plus critique de l'application : il doit garantir qu'une
vente n'est jamais enregistrée sans que le stock correspondant soit
décrémenté (et inversement), même en cas de panne ou d'accès concurrent
de plusieurs caisses.

```mermaid
sequenceDiagram
    actor Caissier
    participant IHM as CaisseController
    participant VS as VenteService
    participant CV as CalculateurVente
    participant DB as Connexion BD (transaction)
    participant VD as VenteDao
    participant SD as StockDao
    participant CD as ClientDao

    Caissier->>IHM: Valider la vente
    IHM->>VS: enregistrerVente(session, depot, client, lignes, paiements)
    VS->>VS: exigerPermission("VENTE_CREER")

    alt panier vide
        VS-->>IHM: IllegalArgumentException
        IHM-->>Caissier: Message d'erreur
    end

    VS->>CV: calculerTotaux(vente)
    CV-->>VS: totalHT, totalTVA, totalTTC

    alt paiement insuffisant
        VS-->>IHM: SupermarcheException
        IHM-->>Caissier: Message d'erreur
    end

    VS->>DB: ouvrir transaction (autoCommit=false)
    VS->>VD: creerVenteEtLignes(cnx, vente)
    VD-->>VS: vente.id généré

    loop pour chaque ligne du panier
        VS->>SD: decrementerPourVente(cnx, produit, depot, qte)
        SD->>SD: SELECT ... FOR UPDATE (verrouille la ligne de stock)
        alt stock insuffisant
            SD-->>VS: StockInsuffisantException
            VS->>DB: rollback()
            VS-->>IHM: exception propagée
            IHM-->>Caissier: Stock insuffisant
        end
        SD->>SD: UPDATE stocks (nouvelle quantité)
        SD->>SD: INSERT mouvements_stock (traçabilité)
    end

    VS->>VD: enregistrerPaiements(cnx, paiements)

    opt client identifié par carte de fidélité
        VS->>CD: ajouterPointsFidelite(cnx, client, points)
    end

    VS->>DB: commit()
    VS-->>IHM: Vente (avec numéro de ticket)
    IHM-->>Caissier: Confirmation + monnaie à rendre
```

## Points clés de robustesse

1. **Transaction unique** : la création de la vente, la décrémentation du stock et l'enregistrement des paiements partagent la même connexion JDBC et ne sont validés (`commit`) qu'une fois toutes les étapes réussies. Toute erreur déclenche un `rollback()` complet.
2. **Verrouillage pessimiste** (`SELECT ... FOR UPDATE`) sur la ligne de stock : si deux caisses tentent de vendre simultanément le dernier exemplaire d'un produit, la seconde transaction attend que la première se termine avant de lire la quantité, ce qui évite un stock négatif.
3. **Calcul des totaux avant toute écriture** : `CalculateurVente` est un calcul pur, exécuté avant l'ouverture de la transaction, ce qui permet de rejeter une vente invalide (paiement insuffisant) sans jamais toucher la base.
