# Diagramme de séquence — Authentification et contrôle d'accès

```mermaid
sequenceDiagram
    actor Utilisateur
    participant IHM as ConnexionController
    participant AS as AuthService
    participant UD as UtilisateurDao
    participant Hash as PasswordHasher
    participant Audit as AuditService

    Utilisateur->>IHM: Saisit identifiant + mot de passe
    IHM->>AS: seConnecter(login, motDePasse)
    AS->>UD: trouverParNomUtilisateur(login)

    alt utilisateur inconnu
        UD-->>AS: Optional.empty()
        AS->>Audit: enregistrer CONNEXION_ECHOUEE
        AS-->>IHM: AuthentificationException
        IHM-->>Utilisateur: Identifiant ou mot de passe incorrect
        note over IHM,Utilisateur: Message volontairement identique au cas mot de passe incorrect (anti-énumération de comptes)
    end

    UD-->>AS: Utilisateur trouvé

    alt compte verrouillé
        AS->>Audit: enregistrer CONNEXION_REFUSEE_VERROUILLE
        AS-->>IHM: AuthentificationException
        IHM-->>Utilisateur: Compte verrouille
    end

    alt compte inactif
        AS-->>IHM: AuthentificationException
        IHM-->>Utilisateur: Compte desactive
    end

    AS->>Hash: verifier(motDePasseClair, hashStocke)

    alt mot de passe incorrect
        Hash-->>AS: false
        AS->>UD: incrementerTentativesEchouees(id, seuil)
        note right of UD: Verrouille automatiquement le compte si le seuil configure est atteint
        AS->>Audit: enregistrer CONNEXION_ECHOUEE
        AS-->>IHM: AuthentificationException
        IHM-->>Utilisateur: Identifiant ou mot de passe incorrect
    end

    Hash-->>AS: true
    AS->>UD: enregistrerConnexionReussie(id)
    AS->>Audit: enregistrer CONNEXION_REUSSIE
    AS-->>IHM: Utilisateur avec role et permissions
    IHM-->>Utilisateur: Acces a l'application principale
```

## Diagramme de séquence — Vérification d'une permission

Ce contrôle est exécuté par chaque méthode sensible des services métier
(`ProduitService`, `VenteService`, etc.), **indépendamment** de ce que
l'interface affiche ou masque.

```mermaid
sequenceDiagram
    participant Service as Service métier ex ProduitService
    participant AS as AuthService
    participant Audit as AuditService

    Service->>AS: exigerPermission PRODUIT_GERER

    alt aucun utilisateur connecte
        AS-->>Service: AuthentificationException
    end

    AS->>AS: utilisateurConnecte.possedePermission(code)

    alt permission absente du role
        AS->>Audit: enregistrer ACCES_REFUSE
        AS-->>Service: AutorisationException
    end

    AS-->>Service: aucune exception, acces autorise
```
