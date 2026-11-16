# F4 : Recherche d'offres

| | |
|---|---|
| Responsable | Fatou SARR (`@SARR-Fatou`) |
| Version visée | v0.1.0 |
| Statut | Idéation |

## 1. Idéation

### Besoin

Un candidat perd du temps à chercher des offres sur plusieurs sites, puis à recopier à la main celles qui
l'intéressent pour les suivre. Il veut trouver des offres d'emploi, de stage et d'alternance depuis l'application,
et y enregistrer en un clic une offre vue ailleurs, à partir de son lien. Les offres enregistrées servent ensuite
aux autres features : adaptation du CV, lettre de motivation et suivi des candidatures.

### Scénarios d'usage

- *En tant que* candidat, je recherche des offres par mots-clés, lieu et type de contrat **afin de** trouver celles
  qui correspondent à mon profil sans quitter l'application.
- *En tant que* candidat, j'enregistre une offre trouvée dans les résultats **afin de** la retrouver et de préparer
  ma candidature.
- *En tant que* candidat, je colle le lien d'une offre vue sur un autre site **afin que** l'application en remplisse
  les informations (titre, entreprise, lieu, description) à ma place.
- *En tant que* candidat, je corrige ou complète une offre importée **afin de** garder des informations justes
  quand l'import n'a pas tout trouvé.
- *En tant que* candidat, je consulte et je supprime mes offres enregistrées.

### Fonctionnement

**Source d'offres : API « Offres d'emploi v2 » de France Travail** (gratuite, inscription sur francetravail.io).

1. **Authentification** : le back-end obtient un jeton OAuth2 (*client credentials*) avec l'identifiant et le
   secret de l'application France Travail. Le jeton est gardé en mémoire jusqu'à son expiration, puis renouvelé.
   Les identifiants sont lus dans des variables d'environnement, jamais commités.
2. **Recherche** : la requête du candidat (mots-clés, lieu, type de contrat, page) est traduite en paramètres de
   l'API France Travail, appelée via `WebClient`. La réponse est convertie en objets `OffreResume` propres à notre
   application : le reste du code ne dépend pas du format de France Travail.
   Les résultats ne sont **pas** enregistrés en base : ils sont seulement affichés.
3. **Enregistrement depuis les résultats** : le candidat choisit une offre ; le back-end récupère son détail
   (`GET /offres/{id}` chez France Travail) et l'enregistre en base, liée au candidat.
4. **Import depuis un lien** :
   - lien France Travail → on extrait l'identifiant de l'offre de l'URL et on passe par l'API (étape 3) ;
   - autre site → on télécharge la page et on cherche les données structurées `JobPosting` (format schema.org,
     présent dans le code de nombreux sites d'offres) ; on en tire titre, entreprise, lieu, type de contrat,
     description et date ;
   - si rien n'est trouvé → l'offre est créée avec le lien seul, et le candidat complète les champs à la main.
5. **Doublons** : une offre déjà enregistrée par le candidat (même source et même identifiant, ou même URL) n'est
   pas créée deux fois : on renvoie l'offre existante.

Pour pouvoir ajouter d'autres sites plus tard (Adzuna, etc.), chaque source implémente une même interface
`SourceOffres` (pattern **Strategy**), et chaque façon d'importer un lien implémente `ImporteurOffre`.

**Exemple** : recherche `motsCles=développeur java`, `lieu=92`, `typeContrat=ALTERNANCE`, page 1
→ 20 offres résumées (titre, entreprise, ville, contrat, date, lien) et le nombre total de résultats.

### Impact sur les couches

- **Base de données** : table `offre`
  (`id`, `utilisateur_id`, `source` [FRANCE_TRAVAIL, LIEN, MANUELLE], `id_externe`, `url`, `titre`, `entreprise`,
  `lieu`, `type_contrat`, `description`, `date_publication`, `date_enregistrement`),
  contrainte d'unicité sur (`utilisateur_id`, `source`, `id_externe`) et sur (`utilisateur_id`, `url`).
- **Back-end** :
  - `SourceOffres` (interface) et `FranceTravailSource` (client de l'API, gestion du jeton) ;
  - `ImporteurOffre` (interface), `ImporteurFranceTravail`, `ImporteurJsonLd` ;
  - `RechercheOffresService`, `OffreService` (enregistrement, doublons, modification, suppression) ;
  - validation des liens importés (voir Limites et sécurité).
- **API REST** :
  - `GET /api/v1/offres/recherche?motsCles=&lieu=&typeContrat=&page=` : rechercher (non enregistré) ;
  - `POST /api/v1/offres` : enregistrer une offre des résultats (`{ "source": "FRANCE_TRAVAIL", "idExterne": "..." }`) ;
  - `POST /api/v1/offres/import` : importer depuis un lien (`{ "url": "https://..." }`) ;
  - `GET /api/v1/offres` : lister mes offres enregistrées ;
  - `GET /api/v1/offres/{id}`, `PUT /api/v1/offres/{id}`, `DELETE /api/v1/offres/{id}`.
- **Interface** : écran « Rechercher des offres » (formulaire, résultats paginés, bouton « Enregistrer »),
  champ « Importer depuis un lien », écran « Mes offres » (liste, détail, modification).

### Critères d'acceptation

- [ ] Une recherche avec des mots-clés renvoie une liste paginée d'offres France Travail, chacune avec un titre,
      un lieu, un type de contrat et un lien vers l'offre d'origine.
- [ ] Une recherche sans résultat renvoie une liste vide (HTTP 200), pas une erreur.
- [ ] Un paramètre invalide (page négative, type de contrat inconnu) renvoie HTTP 400 avec un message clair.
- [ ] Si France Travail ne répond pas ou renvoie une erreur, l'API renvoie HTTP 503 avec un message clair, sans
      exposer la réponse brute ni les identifiants.
- [ ] Enregistrer une offre des résultats la fait apparaître dans « Mes offres ».
- [ ] Importer un lien France Travail crée l'offre avec tous ses champs remplis.
- [ ] Importer un lien d'un site qui fournit un `JobPosting` crée l'offre avec titre, entreprise et description.
- [ ] Importer un lien sans données exploitables crée une offre avec l'URL seule, que le candidat peut compléter.
- [ ] Importer deux fois la même offre ne crée pas de doublon.
- [ ] Un lien qui n'est pas en `http`/`https`, ou qui vise une adresse interne (`localhost`, `127.0.0.1`,
      `192.168.x.x`, etc.), est refusé avec HTTP 400.
- [ ] Un candidat ne voit et ne modifie que ses propres offres.

### Limites

- **Une seule source de recherche** dans cette version : France Travail. D'autres API (Adzuna…) pourront être
  ajoutées grâce à l'interface `SourceOffres`.
- **Sites qui bloquent l'import** : LinkedIn, Indeed et d'autres refusent les accès automatiques ; pour eux,
  l'import tombe sur le cas « URL seule, à compléter ». On ne contourne pas ces protections.
- **Sécurité de l'import (OWASP, SSRF)** : le serveur télécharge une page choisie par l'utilisateur ; on n'accepte
  que `http`/`https`, on refuse les adresses internes et privées, on limite le temps de réponse et la taille de la
  page, et on ne suit qu'un nombre limité de redirections (chacune revérifiée).
- **Quotas** : l'API France Travail limite le nombre d'appels par seconde ; pas de cache dans cette version
  (piste : Redis ou cache Spring si nécessaire).
- **Pas de recherche automatique** ni d'alertes sur de nouvelles offres (piste pour plus tard, avec le Scheduler).
- Le score de correspondance offre / profil relève de la feature *Adaptation du CV*.
- À confirmer avec l'équipe : dépend de l'entité `Utilisateur` et de l'authentification (Spring Security).

## 2. Bilan (à compléter en fin de feature)

### Tests
<!-- Ce qui est testé, et comment. -->

### Usage de l'IA
<!-- Outils utilisés, pour quoi faire, ce qui a été repris, modifié ou rejeté. -->
