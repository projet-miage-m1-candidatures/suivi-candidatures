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

**Mes CV.** Le candidat connecté retrouve ses CV par date de modification décroissante. Chaque carte présente une miniature, le titre, la langue, le modèle, la police, la date de modification et la progression sur sept sections. Le candidat peut filtrer par langue, créer un CV, modifier ou dupliquer un CV existant, le supprimer après confirmation ou le télécharger en PDF.

**Formulaire.** La création et la modification suivent les mêmes huit étapes.

1. **Profil.** Donner un titre au CV, choisir sa langue et saisir le prénom et le nom. Le poste visé, les coordonnées, les liens, le permis, la disponibilité et la mobilité apparaissent dans l’en-tête. Le résumé forme la section Profil.
2. **Compétences.** Regrouper les compétences techniques par thème et ajouter les savoir-être.
3. **Expériences.** Renseigner le poste, l’entreprise, le lieu, la période et la description. Les réalisations suivent la description sous forme de puces.
4. **Projets.** Ajouter un titre, une description et un lien éventuel.
5. **Formation.** Renseigner le diplôme, l’établissement, la mention, la période et la description.
6. **Certifications.** Indiquer le nom, l’organisme, la date d’obtention et un lien éventuel.
7. **Langues.** Indiquer les langues parlées, leur niveau de A1 à C2 ou « Langue maternelle », et les centres d’intérêt.
8. **Vérification.** Relire le récapitulatif et accéder aux sections concernées par les conseils. « Terminer » enregistre le CV et ramène à « Mes CV ».

Le titre du CV, le prénom et le nom sont obligatoires pour enregistrer un brouillon. Les dates de période et de certification acceptent une année (`AAAA`), une année et un mois (`AAAA-MM`) ou une valeur vide. Une expérience avec un début et sans fin s’affiche jusqu’à « aujourd’hui » ou « Present ». La disponibilité est immédiate ou datée au format `AAAA-MM-JJ`.

**Format du contenu.** En plus de ses métadonnées, chaque CV contient un document `contenu`.

| Champ | Données |
|---|---|
| `identite` | Prénom, nom, poste visé, ville, e-mail, téléphone, `mobilite` (texte facultatif de 160 caractères maximum), codes de permis et liens `{ libelle, url }` |
| `profil`, `savoirEtre`, `centresInteret` | Résumé et listes de textes |
| `competences` | Groupes `{ groupe, elements }` |
| `experiences`, `formations` | Périodes `{ debut, fin, enCours }` et descriptions. Les expériences ont leurs réalisations et les formations leur établissement et leur mention. |
| `projets`, `certifications`, `langues` | Projets avec leurs liens et descriptions, certifications avec organisme, date et lien, langues avec leur niveau |
| `disponibilite` | `{ immediate, date }` |

Les listes acceptent jusqu’à 30 éléments. À la lecture d’un ancien CV, le serveur remonte le profil dans `contenu`, transforme le lien unique en entrée de `liens`, renomme `ecole` en `etablissement` et convertit les dates comme `04/2026` en `2026-04`. Les autres dates en texte libre restent visibles à la lecture et dans le PDF. Il faut les mettre au nouveau format ou les effacer avant de réenregistrer le CV. Le nom enregistré est conservé et le prénom reste à renseigner.

**Aperçu et réglages.** `cv-rendu.js` calcule l’aperçu pendant la saisie et utilise le même rendu pour les miniatures. Le choix du modèle applique une police et une couleur par défaut. Le candidat peut les modifier et choisir une taille petite, normale ou grande. La taille normale s’applique par défaut.

| Modèle | Police par défaut | Couleur par défaut |
|---|---|---|
| Classique | Source Serif 4 | Noir |
| Moderne | Inter | Bleu |
| Compact | IBM Plex Sans | Noir |
| Élégant | EB Garamond | Bleu marine |
| Épuré | Lato | Anthracite |

Les neuf polices sont Carlito, Inter, IBM Plex Sans, Lato, Open Sans, Roboto, EB Garamond, Lora et Source Serif 4. Les six thèmes sont Noir, Bleu marine, Bleu, Bordeaux, Vert sapin et Anthracite. Le thème définit la couleur des titres et du texte courant.

**Complétude.** `sectionRemplie()` calcule la progression affichée dans « Mes CV ». Chaque section manquante ouvre directement l’étape correspondante. L’étape Vérification affiche les conseils produits par les règles de `evaluerCv()` dans `cv-rendu.js`, sans IA. Un brouillon incomplet peut être enregistré à tout moment.

**PDF.** Depuis le formulaire, « Télécharger le PDF » enregistre d’abord le CV. Depuis « Mes CV », le bouton télécharge la version enregistrée. Le serveur reprend le HTML de `rendreContenu()` et la feuille `cv-modeles.css`. Dans les modèles à une colonne, les rubriques suivent l’ordre Profil, Compétences, Expériences, Projets, Formation, Certifications, Langues et Centres d’intérêt. Dans Élégant, la colonne de gauche contient Profil, Compétences, Langues, Certifications et Centres d’intérêt. Celle de droite contient Expériences, Projets et Formation. Les liens de l’identité sont visibles et cliquables. Pour les projets et les certifications, le titre est cliquable si le lien utilise HTTP ou HTTPS. Le PDF a sa propre pagination A4. Un bloc d’expérience ou de formation qui tient sur une page reste entier. Un titre de section reste avec le contenu qui suit. La police choisie est intégrée et la génération se fait sans accès au réseau.

### Impact sur les couches

- **Base de données.** Flyway gère la table `cv` dans PostgreSQL. `couleur` et `taille` sont stockées avec le modèle et la police. La migration initialise la couleur des anciens CV selon leur modèle et la taille à Normale, en conservant la police enregistrée. Le champ `contenu` est stocké en `JSONB`, car le CV est un document lu et écrit en entier. La conversion des anciens documents se fait à la lecture.
- **Back-end.** Le package `cv` valide et enregistre les documents, gère les copies et génère le PDF. Les opérations utilisent l’identifiant du candidat connecté fourni par `CandidatCourant.id()`. Si une demande omet la police ou la couleur, le serveur utilise celle du modèle. Si elle omet la taille, il utilise Normale. Il actualise la date de modification à chaque mise à jour.
- **API REST.** `GET` et `POST /api/v1/cv` listent les CV complets et créent un CV. `GET`, `PUT` et `DELETE /api/v1/cv/{id}` lisent, remplacent et suppriment un CV. `POST /api/v1/cv/{id}/copie` en crée une copie. `GET /api/v1/cv/{id}/pdf` renvoie le PDF en pièce jointe. Le navigateur et l’API utilisent la même structure de CV. Les erreurs de l’API sont renvoyées en `ProblemDetail` avec un message lisible dans `detail`.
- **Interface.** `mes-cv.html` et `creer-cv.html` conservent le design validé. `cv-api.js` reste leur seul accès aux données et utilise `appelerApi` pour les requêtes HTTP. `cv-rendu.js` et `cv-modeles.css` produisent l’aperçu et les miniatures.

### Critères d’acceptation

- [ ] Un candidat connecté peut créer un brouillon avec un titre, un prénom et un nom, puis le retrouver et le modifier. Un champ obligatoire vide donne une réponse `400` avec un message lisible.
- [ ] « Mes CV » affiche les miniatures et les filtres de langue. Une modification fait remonter le CV en tête de liste. La duplication crée un CV indépendant avec le suffixe « (copie) ». La suppression confirmée retire le CV de la liste.
- [ ] La liste contient uniquement les CV du candidat connecté. Toute tentative de lire, modifier, supprimer, copier ou exporter le CV d’un autre candidat renvoie `404`.
- [ ] L’aperçu réagit à la saisie et aux changements de langue, de modèle, de police, de couleur et de taille. Chaque modèle applique ses valeurs par défaut. Les réglages choisis sont retrouvés à la réouverture du CV et dans sa copie. Les anciens CV restent consultables et exportables.
- [ ] « Mes CV » affiche la progression et un lien par section manquante. Chaque conseil de Vérification permet d’accéder à la section concernée.
- [ ] Les deux pages permettent de télécharger un fichier A4 nommé `CV-<Prénom>-<Nom>.pdf` en ASCII. Le fichier reprend le contenu, la langue, le modèle, la police, la couleur et la taille de l’aperçu. Depuis le formulaire, il contient les dernières modifications.
- [ ] Sur un CV de plusieurs pages, les expériences et les formations qui tiennent sur une page restent entières. Les titres de section restent avec leur contenu.

### Limites

- Les deux pages permettent de sélectionner ou de glisser un PDF ou un DOCX de 5 Mo maximum. L’extraction et le remplissage automatique à partir de ce fichier sont prévus plus tard, comme l’action « Améliorer avec l’IA ».
