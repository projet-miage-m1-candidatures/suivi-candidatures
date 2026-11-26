# Créer un CV

| | |
|---|---|
| Issue | #17 |
| Responsable | Kyrylo NEBYLYTSIA (`@NebylytsiaKyrylo`) |
| Version visée | v1.0.0 |
| Statut | Idéation |

## 1. Idéation

### Besoin

Un candidat doit pouvoir préparer plusieurs versions de son CV selon le poste visé ou la langue de la candidature. Il doit pouvoir reprendre un brouillon, vérifier la mise en page pendant la saisie et télécharger un PDF à joindre à sa candidature.

### Scénarios d’usage

- *En tant que* candidat, je remplis un formulaire guidé et j’enregistre un brouillon **afin de** reprendre mon CV plus tard.
- *En tant que* candidat, je duplique un CV existant **afin de** l’adapter à une autre offre ou de préparer une version en anglais.
- *En tant que* candidat, je consulte les points à compléter et je relis l’aperçu **afin de** vérifier mon CV avant de le télécharger.

### Fonctionnement

**Mes CV.** Le candidat connecté retrouve ses CV du plus récemment modifié au moins récemment modifié. Chaque carte présente une miniature, le titre du CV, sa langue, son modèle, sa police, sa date de modification et son état de complétude. Le candidat peut filtrer par langue, créer un CV, modifier ou dupliquer un CV existant, le supprimer après confirmation ou le télécharger en PDF.

**Formulaire.** La création et la modification suivent les mêmes six étapes.

1. **Identité.** Donner un titre au CV, choisir le français ou l’anglais, saisir le nom complet et, selon le besoin, renseigner le poste visé, les coordonnées et le profil. Le titre du CV sert à le retrouver dans « Mes CV ». Le poste visé est un champ distinct qui figure sous le nom dans le document.
2. **Expériences.** Ajouter les postes, les entreprises, les lieux et les périodes. Chaque réalisation saisie apparaît sous forme de puce dans le CV.
3. **Formation.** Ajouter les diplômes, les établissements et les périodes.
4. **Compétences.** Regrouper les compétences par thème, par exemple « Langages » pour SQL, Python et Java.
5. **Langues.** Indiquer les langues parlées et leur niveau, de A1 à C2 ou « Langue maternelle ».
6. **Vérification.** Relire le récapitulatif et ouvrir la section à corriger depuis un conseil. « Terminer » enregistre le CV et ramène à « Mes CV ».

Le titre du CV et le nom complet sont obligatoires, y compris pour enregistrer un brouillon. Les périodes d’expérience et de formation ont deux champs de texte libre, `debut` et `fin`. On peut saisir « 03/2025 », « 2026 » ou « En cours ».

**Aperçu, modèles et polices.** `cv-rendu.js` calcule l’aperçu instantanément dans le navigateur et utilise le même rendu pour les miniatures. Le choix du modèle applique sa police par défaut. Le candidat peut ensuite choisir Inter, Lato, IBM Plex Sans, Source Serif 4 ou Lora, quel que soit le modèle.

| Modèle | Police par défaut |
|---|---|
| Classique | Source Serif 4 |
| Moderne | Inter |
| Compact | IBM Plex Sans |

**Complétude.** `evaluerCv()` applique des règles simples dans `cv-rendu.js`, sans IA, par exemple la présence d’un profil, d’une formation et d’au moins cinq compétences. La carte affiche « Complet » si toutes les règles sont satisfaites. Sinon, « À compléter (n) » indique le nombre de points restants et ouvre directement l’étape Vérification. Un brouillon incomplet peut être enregistré à tout moment.

**PDF.** Depuis le formulaire, « Télécharger en PDF » enregistre d’abord le CV. Depuis « Mes CV », le bouton télécharge la version enregistrée. Le serveur reprend les mêmes balises, classes et titres de section FR/EN que `afficherCv()`, ainsi que la même feuille `cv-modeles.css`, pour reproduire le rendu de l’aperçu. Le PDF a sa propre pagination A4. Chaque expérience ou formation reste sur une page (`page-break-inside: avoid` sur `.cv-item`). Un titre de section reste avec le contenu qui suit (`page-break-after: avoid` sur les `h2`). Les polices sont intégrées au fichier et la génération se fait sans accès au réseau.

### Impact sur les couches

- **Base de données.** Flyway crée la table `cv` dans PostgreSQL. Elle stocke le propriétaire, le titre, la langue, le modèle, la police et les dates de création et de modification. Le champ `contenu` est stocké en `JSONB`, car le CV est un document lu et écrit en entier. Il regroupe l’identité, les expériences, les formations, les compétences et les langues.
- **Back-end.** Le package `cv` valide et enregistre les documents, gère les copies et génère le PDF. Les opérations utilisent l’identifiant du candidat connecté fourni par `CandidatCourant.id()`. Le serveur valide les champs et la taille des listes, applique la police par défaut du modèle si elle est absente et actualise la date de modification à chaque mise à jour.
- **API REST.** `GET` et `POST /api/v1/cv` listent les CV complets et créent un CV. `GET`, `PUT` et `DELETE /api/v1/cv/{id}` lisent, remplacent et suppriment un CV. `POST /api/v1/cv/{id}/copie` en crée une copie. `GET /api/v1/cv/{id}/pdf` renvoie le PDF en pièce jointe. Le navigateur et l’API utilisent la même structure de CV. Les erreurs de l’API sont renvoyées en `ProblemDetail` avec un message lisible dans `detail`.
- **Interface.** `mes-cv.html` et `creer-cv.html` conservent le design validé. `cv-api.js` reste leur seul accès aux données et utilise `appelerApi` pour les requêtes HTTP. `cv-rendu.js` et `cv-modeles.css` produisent l’aperçu et les miniatures.

### Critères d’acceptation

- [ ] Un candidat connecté peut créer un brouillon avec un titre et un nom, le retrouver après rechargement et le modifier. Un titre ou un nom vide donne une réponse `400` avec un message lisible.
- [ ] « Mes CV » affiche les miniatures et les filtres de langue. Une modification fait remonter le CV en tête de liste. La duplication crée un CV indépendant avec le suffixe « (copie) ». La suppression confirmée retire le CV de la liste.
- [ ] La liste contient uniquement les CV du candidat connecté. Toute tentative de lire, modifier, supprimer, copier ou exporter le CV d’un autre candidat renvoie `404`.
- [ ] L’aperçu réagit à la saisie et aux changements de langue, de modèle et de police. Chacun des trois modèles applique sa police par défaut et accepte les cinq polices proposées. Le modèle, la police et les périodes en texte libre sont retrouvés à la réouverture du CV.
- [ ] Le badge « À compléter (n) » ouvre directement Vérification. Chaque conseil permet d’accéder à la section concernée. Une fois toutes les règles satisfaites, le badge affiche « Complet ».
- [ ] Les deux pages permettent de télécharger un fichier A4 nommé `CV-<Nom>.pdf`, avec un nom de fichier en ASCII. Le fichier reprend le contenu, les titres de section dans la langue choisie, le modèle et la police de l’aperçu. Depuis le formulaire, il contient les dernières modifications.
- [ ] Sur un CV de plusieurs pages, chaque expérience et chaque formation tient sur une page et aucun titre de section ne reste seul en bas de page.

### Limites

- L’action « Améliorer avec l’IA » et l’import d’un CV au format PDF sont des évolutions prévues hors de l’issue #17.
- Le badge de complétude indique le résultat des règles de saisie. Il ne mesure pas l’adéquation du CV à une offre.
