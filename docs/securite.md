# Sécurité

## Authentification

- Mots de passe hachés avec **BCrypt** (coût configurable, 12 par défaut via `application.yml` → `security.bcryptStrength`). Aucun mot de passe en clair n'est jamais stocké, journalisé ou affiché.
- **Verrouillage automatique** d'un compte après un nombre configurable de tentatives échouées (`security.maxLoginAttempts`, 5 par défaut). Le déverrouillage nécessite l'intervention d'un administrateur (module Utilisateurs).
- Les messages d'erreur de connexion sont **volontairement identiques** entre "identifiant inconnu" et "mot de passe incorrect", pour ne pas permettre à un attaquant de déterminer quels comptes existent (anti-énumération).
- Politique de mot de passe imposée à la création/au changement : minimum 10 caractères, au moins une majuscule, une minuscule et un chiffre (`AuthService.validerForceMotDePasse`).

## Autorisation (rôles et permissions)

Le modèle est **RBAC** (Role-Based Access Control) à deux niveaux :

| Rôle | Permissions par défaut |
|---|---|
| ADMIN | Toutes (y compris gestion des utilisateurs) |
| GERANT | Toutes sauf gestion des utilisateurs |
| CAISSIER | Vente, caisse, gestion clients |
| MAGASINIER | Stock, fournisseurs, produits |

Chaque action sensible est protégée par un contrôle explicite dans la couche service :

```java
public void creerProduit(Produit produit) {
    authService.exigerPermission("PRODUIT_GERER");
    // ...
}
```

**Ce contrôle est la véritable barrière de sécurité.** L'interface masque aussi les boutons correspondants (`PrincipalController.appliquerVisibiliteSelonPermissions`), mais ce n'est qu'un confort d'usage : même un appel direct au service depuis un autre point de code serait bloqué.

Le détail des permissions et leur attribution par rôle est défini dans `database/02_donnees_initiales.sql`, modifiable sans changement de code (ajout de permissions, réattribution).

## Journalisation / audit

Toute action sensible est tracée dans la table `journal_audit` via `AuditService` : connexions (réussies et échouées), créations/modifications de produits, ventes, ajustements de stock, changements de mots de passe, accès refusés, etc.

L'écriture du journal est **best-effort** : une panne d'écriture du journal n'empêche jamais une opération métier de se terminer (voir commentaire dans `AuditService`), pour ne pas bloquer la caisse en cas de problème mineur d'infrastructure.

## Protection des données sensibles

- Les identifiants de connexion à la base de données ne sont **jamais codés en dur**. Ils sont lus depuis `application.yml`, surchargeable par variables d'environnement (`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`) qui ont toujours priorité — c'est la méthode recommandée en production.
- `application-local.yml` et `.env` sont exclus du contrôle de version (`.gitignore`) pour éviter qu'un secret de production ne soit committé par erreur.
- Le script `database/02_donnees_initiales.sql` ne contient **aucun mot de passe par défaut pré-rempli** : le hash du compte administrateur initial doit être généré localement (voir `PasswordHasher.main`) et copié manuellement avant exécution.

## Concurrence et intégrité des données

Le verrouillage pessimiste (`SELECT ... FOR UPDATE`) sur les lignes de stock empêche deux caisses de vendre simultanément un même article au-delà du stock disponible (voir `diagramme_sequence_vente.md`). Toute opération composite (vente + stock + paiement, réception de commande + stock) est exécutée dans une transaction unique avec rollback automatique en cas d'erreur partielle.

## Limites connues et pistes d'amélioration

- Pas de chiffrement TLS configuré par défaut sur la connexion JDBC vers MariaDB : à activer (paramètre `useSSL=true` côté URL JDBC) si le réseau local n'est pas considéré de confiance.
- Pas d'expiration automatique des mots de passe (renouvellement périodique imposé) : à ajouter si la politique de sécurité de l'entreprise l'exige.
- Le journal d'audit n'a pas de mécanisme d'archivage/purge automatique : à prévoir pour la conformité RGPD si des données personnelles (noms de clients) y apparaissent indirectement sur le long terme.
