---
title: "Suivi de candidatures"
subtitle: "Dossier de lancement · Projet S7 · M1 MIAGE, Université Paris Nanterre"
author:
  - Kyrylo NEBYLYTSIA
  - Fatou SARR
  - Laura MRIDHA
date: 16 novembre 2026
---

<!--
  Dossier de lancement (Rendu 1) : 4 pages, puis il s'enrichit à chaque livraison.
  Compilation automatique par GitHub Actions (make -C doc : pandoc + WeasyPrint, mise en page dans style.css).
  Les commentaires HTML comme celui-ci n'apparaissent pas dans le PDF.
  Pour signaler un passage à écrire : [À compléter]{.todo}
  Pour étiqueter une feature : [F1]{.tag}
-->

# Équipe, comptes développeurs et dépôt

| Membre | Compte GitHub |
|--------|---------------|
| Kyrylo NEBYLYTSIA | <https://github.com/NebylytsiaKyrylo> |
| Fatou SARR | <https://github.com/SARR-Fatou> |
| Laura MRIDHA | <https://github.com/sharikamrdh> |

**Dépôt** : <https://github.com/projet-miage-m1-candidatures/suivi-candidatures>

# Projet et caractère innovant

Une application web intelligente qui permet aux candidats de rechercher des offres d'emploi, de stage et
d'alternance, de préparer leurs candidatures avec l'aide de l'IA, et de suivre leur évolution depuis un
tableau de bord centralisé.

- **Trouver** des offres correspondant à son profil.
- **Préparer** ses candidatures avec l'IA : créer, améliorer et adapter son CV à chaque offre, et rédiger
  automatiquement sa lettre de motivation.
- **Suivre** chaque candidature, de l'envoi à la réponse : statut, historique et tableau de bord.

> **Caractère innovant.** Là où les outils existants ne couvrent qu'une partie de la recherche, notre
> application réunit toutes les étapes, de l'offre à la réponse, et utilise l'IA pour adapter chaque
> candidature à l'offre visée.

# Scénarios d'usage

Le diagramme présente tout ce qu'un candidat peut faire dans l'application. Les services externes restent
à choisir : une ou plusieurs API de sites d'offres, et une IA accessible par API ou en modèle local.

![Cas d'utilisation du candidat](diagrammes/cas-utilisation.png)

# Répartition des features

Chaque feature est portée par **une seule personne**, de bout en bout : base de données, back-end, API REST
et interface. Avant de coder, elle est décrite dans une fiche du dossier `doc/features/`.

| Membre | Feature | Description | Fiche |
|--------|---------|-------------|-------|
| Kyrylo NEBYLYTSIA | Adaptation du CV par l'IA | analyse du CV face à une offre, score de correspondance, suggestions pour l'améliorer | [adaptation-cv.md](https://github.com/projet-miage-m1-candidatures/suivi-candidatures/blob/main/doc/features/adaptation-cv.md) |
| Kyrylo NEBYLYTSIA | Lettre de motivation | analyse de l'offre et du CV, rédaction d'une lettre de motivation adaptée | [lettre-motivation.md](https://github.com/projet-miage-m1-candidatures/suivi-candidatures/blob/main/doc/features/lettre-motivation.md) |
| Kyrylo NEBYLYTSIA | Parcours d'accueil (onboarding) [À discuter]{.todo} | questionnaire guidé à la création du compte : poste visé, expérience, lieu, mode de travail, CV | [onboarding.md](https://github.com/projet-miage-m1-candidatures/suivi-candidatures/blob/main/doc/features/onboarding.md) |
| Fatou SARR | Recherche d'offres | recherche via les API de sites d'offres, import d'une offre depuis un lien | [recherche-offres.md](https://github.com/projet-miage-m1-candidatures/suivi-candidatures/blob/main/doc/features/recherche-offres.md) |
| Laura MRIDHA | Suivi des candidatures | statuts, historique, tableau de bord | [suivi-candidatures.md](https://github.com/projet-miage-m1-candidatures/suivi-candidatures/blob/main/doc/features/suivi-candidatures.md) |

# Architecture

![Architecture générale : front, API REST, back, base de données et services externes](diagrammes/architecture.png)

| Couche | Choix envisagé | Remarques |
|--------|----------------|-----------|
| Front | HTML, CSS, JavaScript (Thymeleaf à confirmer) | consomme uniquement l'API REST |
| API REST | Spring Boot (Spring MVC), OpenAPI / Swagger UI | endpoints versionnés sous `/api/v1` |
| Back | Java 21, Spring Boot, Spring Data JPA, Bean Validation | `@Async` / `CompletableFuture`, Scheduler Spring (rappels, relances) |
| Documents | Apache PDFBox (PDF), Apache POI (Word) | extraction du texte des CV |
| Services externes | IA (API d'un LLM ou modèle local), API de sites d'offres, appelées via WebClient | adaptation des CV, lettres de motivation, récupération des offres |
| BD | PostgreSQL [À discuter]{.todo} | pistes : MongoDB pour les offres (champs variables selon les sources), Redis pour le cache |
| Sécurité | Spring Security | authentification |
| Qualité | JUnit 5, AssertJ, JaCoCo, SonarQube | intégration continue GitHub Actions |
| Conteneurs | Docker Compose | base de données dès v0.1.0 |

> **Première version, à discuter en équipe.** Le choix des bases de données, en particulier, reste ouvert.

**Qualité du code.**

- **Clean Code** : noms métier explicites, fonctions courtes, pas de duplication, commentaires qui expliquent
  le *pourquoi* ;
- **SOLID**, objets immuables et règles vérifiées à la construction ;
- **design patterns** quand ils servent : *State* (statuts des candidatures), *Observer* (notifications),
  *Strategy* (comparaison CV / offre) ;
- conventions **REST** et règles de sécurité **OWASP** ;
- analyse **SonarQube** au vert.

# Démarche pour chaque feature

Notre façon de travailler est décrite dans `CONTRIBUTING.md`, à la racine du dépôt :

- **une feature, une personne**, de bout en bout ;
- **idéation d'abord** : avant de coder, chaque feature est décrite dans une fiche `doc/features/`, à partir
  du modèle `MODELE-feature.md`, et validée par la relecture d'un autre membre ;
- **GitHub flow** : `main` est protégée, tout passe par une branche et une pull request relue.

<!-- Compléter avec les étapes de développement (BD, back, tests, API, interface) une fois l'architecture choisie. -->

# Protocole d'usage de l'IA

**Principe : l'IA assiste, l'humain reste l'auteur.** On ne laisse jamais l'IA tout faire sans comprendre :
tout ce qui est commité a été relu et compris par son auteur, capable de l'expliquer, de le modifier et de
le refaire sans aide.

| Activité | Ce que fait l'IA | Ce que fait l'humain |
|----------|------------------|----------------------|
| Idéation et conception | Critique les idées, propose des options | Choisit, justifie, rédige la fiche |
| Algorithme | Explique, relit, signale les cas oubliés | **Conçoit et écrit le cœur de l'algorithme** |
| Code technique | Peut générer un premier jet | Relit chaque ligne, adapte, assume |
| Tests | Suggère des cas | Définit cas limites et propriétés ; vérifie qu'un test échoue quand on casse le code |
| CI/CD, conteneurisation | Peut générer workflows et Dockerfile | Comprend chaque étape, vérifie l'exécution, gère les secrets |
| Documentation | Aide à rédiger et mettre en forme | Valide le contenu, vérifie les faits |

**Interdits.** Commiter du code qu'on ne comprend pas ; confier des secrets ou des données personnelles à
l'IA ; commiter sous une autre identité ; présenter comme humain un travail de l'IA.
