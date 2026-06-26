# Système de gestion de supermarché

Application desktop Java/JavaFX pour la gestion complète d'un supermarché : produits, stocks, fournisseurs, caisse, clients, employés, reporting et sécurité.

## Sommaire de la documentation

- [`docs/architecture.md`](docs/architecture.md) — Vue en couches et déploiement réseau
- [`docs/modele_donnees.md`](docs/modele_donnees.md) — Modèle entité-relation
- [`docs/diagramme_classes.md`](docs/diagramme_classes.md) — Diagramme de classes (modèle métier)
- [`docs/diagramme_cas_utilisation.md`](docs/diagramme_cas_utilisation.md) — Cas d'utilisation par rôle
- [`docs/diagramme_sequence_vente.md`](docs/diagramme_sequence_vente.md) — Flux d'enregistrement d'une vente
- [`docs/diagramme_sequence_authentification.md`](docs/diagramme_sequence_authentification.md) — Flux d'authentification
- [`docs/securite.md`](docs/securite.md) — Détail des mesures de sécurité

Les diagrammes sont au format Mermaid : ils s'affichent automatiquement sur GitHub/GitLab, ou peuvent être collés dans mermaid.live pour être visualisés/exportés en image.

## Prérequis

- Java 21 (JDK complet, pas seulement le JRE)
- Maven 3.8+
- MariaDB 10.6+ (ou MySQL 8+, compatible) déjà installé et accessible sur le réseau

Vérifiez vos versions :

```bash
java -version
mvn -version
```

## Installation

### 1. Créer la base de données

Connectez-vous à votre serveur MariaDB et exécutez le script de schéma :

```bash
mysql -u root -p < database/01_schema.sql
```

Cela crée la base `supermarche` avec toutes ses tables (produits, stocks, ventes, utilisateurs, etc.).

### 2. Créer un utilisateur dédié à l'application (recommandé)

Ne faites pas tourner l'application avec le compte root de MariaDB. Créez un compte dédié avec les droits minimaux nécessaires :

```sql
CREATE USER 'supermarche_app'@'%' IDENTIFIED BY 'UnMotDePasseFortIci';
GRANT SELECT, INSERT, UPDATE, DELETE ON supermarche.* TO 'supermarche_app'@'%';
FLUSH PRIVILEGES;
```

Adaptez `'%'` à l'adresse IP de vos postes de caisse si vous voulez restreindre l'accès réseau.

### 3. Configurer la connexion

Deux options :

**Option A — variables d'environnement (recommandé en production)** :

```bash
export DB_HOST=192.168.1.10
export DB_PORT=3306
export DB_NAME=supermarche
export DB_USER=supermarche_app
export DB_PASSWORD=UnMotDePasseFortIci
```

**Option B — fichier de configuration local** : copiez `src/main/resources/config/application.yml` en `application-local.yml` (déjà exclu du contrôle de version) et adaptez les valeurs. Notez que cela nécessite d'adapter le nom de fichier chargé dans `AppConfig` si vous utilisez cette option, car par défaut l'application charge `application.yml`.

### 4. Générer le mot de passe administrateur initial

Le script de données initiales ne contient volontairement aucun mot de passe pré-rempli (voir `docs/securite.md`). Compilez d'abord le projet :

```bash
mvn compile
```

Puis générez un hash pour votre mot de passe administrateur choisi :

```bash
mvn exec:java -Dexec.mainClass="com.supermarche.security.PasswordHasher" -Dexec.args="VotreMotDePasseSolide1"
```

Copiez le hash affiché (commence par `$2a$` ou `$2b$`) dans `database/02_donnees_initiales.sql`, à la place de `<HASH_BCRYPT_A_GENERER>`.

### 5. Charger les données initiales

```bash
mysql -u root -p supermarche < database/02_donnees_initiales.sql
```

Cela crée les rôles, permissions, le dépôt et les caisses par défaut, ainsi que votre compte administrateur (identifiant `admin`, mot de passe celui choisi à l'étape 4).

### 6. Lancer l'application

```bash
mvn javafx:run
```

Ou pour générer un jar exécutable autonome à distribuer sur les postes de caisse :

```bash
mvn package
java -jar target/gestion-supermarche-1.0.0.jar
```

Le nom exact du jar dépend de la configuration du plugin shade ; vérifiez le contenu de `target/` après `mvn package`.

## Premiers pas dans l'application

1. Connectez-vous avec le compte `admin` créé à l'étape 4.
2. Allez dans Utilisateurs pour créer les comptes des employés (caissiers, magasiniers, gérants) avec le rôle approprié.
3. Allez dans Produits pour créer votre catalogue.
4. Allez dans Fournisseurs pour enregistrer vos fournisseurs.
5. Allez dans Stocks pour saisir les quantités initiales en stock (entrée manuelle).
6. Sur chaque poste de caisse, allez dans Caisse, sélectionnez la caisse physique correspondante, saisissez le fond de caisse, et ouvrez la session pour commencer à vendre.

## Exécuter les tests

```bash
mvn test
```

Les tests couvrent la logique critique : authentification et verrouillage de compte (AuthServiceTest), calculs de totaux de vente (CalculateurVenteTest), validation métier des ventes (VenteServiceTest), et gestion concurrente du stock (StockDaoTest). Voir le détail dans le code source de src/test/java.

## Limites connues de cette version

- Pas d'import en masse de catalogue produit (CSV/Excel) : la saisie se fait actuellement produit par produit dans l'IHM.
- Pas d'intégration matérielle caisse (scanner, tiroir-caisse, imprimante ticket) : la saisie de code-barres se fait au clavier, et le ticket s'affiche à l'écran plutôt que de s'imprimer physiquement. L'architecture du module Caisse a été pensée pour permettre cet ajout sans réécriture majeure.
- Pas de mode hors-ligne : chaque poste nécessite une connexion réseau active vers le serveur MariaDB.
- Un seul dépôt/magasin actif par défaut dans les données initiales ; le schéma supporte plusieurs dépôts mais l'IHM ne propose pas encore de bascule explicite entre dépôts pour le module Stocks.

## Structure du projet

```
supermarche-app/
├── database/                  Scripts SQL (schéma + données initiales)
├── docs/                       Documentation et diagrammes UML (Mermaid)
├── src/main/java/com/supermarche/
│   ├── model/                  Entités métier
│   ├── dao/                    Accès aux données (JDBC)
│   ├── service/                Logique métier et règles de gestion
│   ├── security/                Hachage des mots de passe
│   ├── controller/              Contrôleurs JavaFX
│   ├── config/                  Configuration (BD, contexte applicatif)
│   ├── exception/               Exceptions métier
│   └── util/                    Utilitaires (dialogues, formatage)
├── src/main/resources/
│   ├── fxml/                    Écrans JavaFX
│   ├── css/                     Thème visuel
│   └── config/                  application.yml
├── src/test/java/                Tests unitaires
└── pom.xml
```
