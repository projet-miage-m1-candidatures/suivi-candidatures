# Développer le projet

Ce guide explique comment préparer, lancer et tester le projet.
Pour les branches, les commits et les pull requests, consulte [CONTRIBUTING.md](CONTRIBUTING.md).

## 1. Prérequis

Installe les outils suivants :

- **Java 21** ;
- **Git** ;
- **Docker** avec Docker Compose ;
- un **IDE** compatible avec Java et Gradle.

Il n'est pas nécessaire d'installer Gradle ou PostgreSQL. Le projet fournit le Gradle Wrapper et Docker lance la base de données.

## 2. Préparer le projet

Après avoir cloné le dépôt, crée le fichier `.env` à partir du modèle :

```bash
cp .env.example .env
```

Sous Windows, utilise `copy` à la place de `cp`.

Dans `.env`, remplace `change-moi` par un mot de passe local. Ce fichier contient des informations sensibles : Git l'ignore et il ne doit jamais être commité.

### Assistant IA (facultatif)

L'application fonctionne par défaut en mode `faux`, sans clé API. Pour utiliser OpenAI, demande à Kyrylo
une invitation dans le projet OpenAI de l'équipe et crée ta propre clé. Dans ton fichier `.env`,
configure `IA_FOURNISSEUR=openai`, `IA_MODELE=gpt-6-luna` et renseigne `IA_CLE` avec ta clé.
Garde ta clé privée et masque-la sur toute capture d'écran. Consulte la
[fiche de l'assistant IA](doc/features/assistant-ia.md).

## 3. Lancer et arrêter l'application

Démarre PostgreSQL :

```bash
docker compose up -d
```

Puis démarre l'application :

```bash
./gradlew bootRun
```

Sous Windows, remplace `./gradlew` par `gradlew.bat`.

Vérifie ensuite l'état de l'application à l'adresse <http://localhost:8080/actuator/health>. La réponse doit contenir `"status":"UP"`.

Pour arrêter l'application :

1. utilise `Ctrl + C` dans son terminal ;
2. arrête PostgreSQL avec la commande suivante :

```bash
docker compose down
```

Cette commande conserve les données locales de PostgreSQL.

## 4. Structure des packages

Le code Java se trouve dans `src/main/java/fr/miage/suivicandidatures`.

| Package | Rôle |
|---|---|
| `commun` | Éléments partagés : configuration, gestion des erreurs et utilisateur |
| `cv` | Préparation du CV, analyse d'une offre et lettre de motivation |
| `offres` | Recherche, import et enregistrement des offres |
| `candidatures` | Suivi des candidatures et de leurs statuts |

Le projet est organisé par **fonctionnalité**. Chaque nouvelle fonctionnalité peut avoir son propre package. Elle regroupe les classes qui couvrent ses différentes couches :

| Type de classe | Rôle |
|---|---|
| `Controller` | Reçoit les requêtes HTTP de l'API REST |
| `Service` | Contient les règles métier |
| `Repository` | Accède aux données avec JPA |
| `Entity` | Représente les données enregistrées en base |

Les éléments réellement partagés entre plusieurs fonctionnalités vont dans `commun`.

## 5. Gérer la base avec Flyway

Flyway applique les modifications de la base à partir de fichiers SQL appelés **migrations**. Ces fichiers garantissent que chaque membre de l'équipe et la CI utilisent la même structure de base.

Les migrations se trouvent dans `src/main/resources/db/migration`. Ce dossier sera créé avec la première migration.

Pour ajouter ou modifier une table :

1. crée un fichier SQL dans ce dossier ;
2. nomme-le sous la forme `V<date>_<heure>__<description>.sql`, par exemple `V20261118_1430__cree_table_offre.sql` ;
3. relance l'application pour que Flyway exécute la migration.

Une migration déjà fusionnée dans `main` ne doit jamais être modifiée. Pour faire évoluer une table, ajoute une nouvelle migration avec une commande comme `ALTER TABLE`.

Hibernate ne crée pas les tables. Il vérifie que les entités Java correspondent à la structure créée par Flyway.

## 6. Lancer les tests

La base PostgreSQL doit être démarrée. Lance ensuite les tests :

```bash
./gradlew test
```

Sous Windows :

```bash
gradlew.bat test
```

La CI lance également les tests pour chaque pull request.
