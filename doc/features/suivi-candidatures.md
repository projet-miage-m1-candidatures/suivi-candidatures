# F5 : Suivi des candidatures

| | |
|---|---|
| Responsable | Laura MRIDHA (`@sharikamrdh`) |
| Version visée | v0.1.0 |
| Statut | Idéation |

## 1. Idéation

### Besoin

Un candidat envoie beaucoup de candidatures et finit par ne plus savoir laquelle a reçu une réponse, laquelle doit
être relancée, ni où il en est globalement. Il veut enregistrer chaque candidature, faire évoluer son statut au fil
des réponses, retrouver ce qui s'est passé et quand, et voir sa recherche d'un seul coup d'œil.

### Scénarios d'usage

- *En tant que* candidat, je clique sur « J'ai postulé » sur une offre enregistrée **afin que** la candidature soit
  ajoutée automatiquement à mon suivi, sans rien avoir à saisir.
- *En tant que* candidat, je change le statut d'une candidature (par exemple « Entretien ») **afin que** l'application
  reflète où j'en suis.
- *En tant que* candidat, j'ajoute une note à un changement de statut **afin de** me rappeler du contexte (nom du
  recruteur, date de l'entretien).
- *En tant que* candidat, je consulte l'historique d'une candidature **afin de** savoir quand chaque étape a eu lieu.
- *En tant que* candidat, je liste mes candidatures et je les filtre par statut **afin de** retrouver rapidement celles
  qui demandent une action.
- *En tant que* candidat, j'ouvre mon tableau de bord **afin de** voir combien de candidatures sont à chaque étape.

### Fonctionnement

1. **Création automatique** : quand le candidat clique sur « J'ai postulé » sur une offre enregistrée, une
   candidature est créée sans aucune saisie. Elle reprend les informations de l'offre (titre, entreprise, lieu), prend
   le statut `ENVOYEE` et la date du jour, et la première ligne de l'historique est ajoutée. Une même offre ne peut
   donner lieu qu'à une seule candidature par candidat.
2. **Changement de statut** : le candidat choisit un nouveau statut, avec une note facultative. Le back-end vérifie que
   la transition est autorisée (voir ci-dessous), met à jour le statut et ajoute une ligne à l'historique.
3. **Historique** : chaque changement de statut est conservé (ancien statut, nouveau statut, date, note). Il n'est
   jamais modifié ni supprimé.
4. **Tableau de bord** : le back-end compte les candidatures du candidat par statut et renvoie aussi les dernières
   candidatures modifiées.

**Statuts** : `ENVOYEE`, `RELANCEE`, `ENTRETIEN`, `OFFRE_RECUE`, `ACCEPTEE`, `REFUSEE`, `SANS_REPONSE`.

**Transitions autorisées** (pattern **State**) :

| Depuis | Vers |
|--------|------|
| `ENVOYEE` | `RELANCEE`, `ENTRETIEN`, `REFUSEE`, `SANS_REPONSE` |
| `RELANCEE` | `ENTRETIEN`, `REFUSEE`, `SANS_REPONSE` |
| `ENTRETIEN` | `OFFRE_RECUE`, `REFUSEE` |
| `OFFRE_RECUE` | `ACCEPTEE`, `REFUSEE` |
| `SANS_REPONSE` | `ENTRETIEN`, `REFUSEE` (réponse tardive) |
| `ACCEPTEE`, `REFUSEE` | aucune : statuts finaux |

**Exemple** : une candidature `ENVOYEE` passe à `ENTRETIEN` avec la note « Entretien jeudi 14 h ». L'historique contient
deux lignes : la création, puis ce passage. Le tableau de bord affiche 1 candidature en `ENTRETIEN`.

### Impact sur les couches

- **Base de données** : deux tables.
  - `candidature` (`id`, `utilisateur_id`, `offre_id`, `statut`, `date_candidature`, `date_modification`),
    unicité sur (`utilisateur_id`, `offre_id`) ;
  - `historique_statut` (`id`, `candidature_id`, `ancien_statut`, `nouveau_statut`, `date_changement`, `note`).
- **Back-end** :
  - énumération `StatutCandidature` et règles de transition (pattern State) ;
  - `CandidatureService` (création, changement de statut, consultation, suppression) ;
  - `TableauDeBordService` (comptage par statut, dernières candidatures) ;
  - validation des entrées (Bean Validation).
- **API REST** :
  - `POST /api/v1/candidatures` : créer à partir d'une offre (`{ "offreId": 12 }`), appelé par le bouton
    « J'ai postulé » ;
  - `GET /api/v1/candidatures?statut=` : lister mes candidatures ;
  - `GET /api/v1/candidatures/{id}` : détail ;
  - `PATCH /api/v1/candidatures/{id}/statut` : changer de statut (`{ "statut": "ENTRETIEN", "note": "..." }`) ;
  - `GET /api/v1/candidatures/{id}/historique` : historique ;
  - `DELETE /api/v1/candidatures/{id}` : supprimer ;
  - `GET /api/v1/tableau-de-bord` : comptages par statut et dernières candidatures.
- **Interface** : bouton « J'ai postulé » sur chaque offre enregistrée, écran « Mes candidatures » (liste, filtre par statut), écran de détail avec historique et bouton de
  changement de statut, et écran « Tableau de bord ».

### Critères d'acceptation

- [ ] Cliquer sur « J'ai postulé » crée la candidature (HTTP 201) avec le statut `ENVOYEE`, les informations de
      l'offre et la première ligne d'historique, sans autre saisie.
- [ ] Créer une seconde candidature pour la même offre renvoie HTTP 409.
- [ ] Créer une candidature pour une offre inexistante, ou appartenant à un autre candidat, renvoie HTTP 404.
- [ ] Un changement de statut autorisé met à jour la candidature et ajoute une ligne à l'historique.
- [ ] Un changement de statut non autorisé (par exemple `ENVOYEE` vers `ACCEPTEE`) renvoie HTTP 409 avec un message
      clair, et ne modifie rien.
- [ ] Une candidature `ACCEPTEE` ou `REFUSEE` ne peut plus changer de statut.
- [ ] Un statut inconnu renvoie HTTP 400.
- [ ] L'historique est renvoyé du plus récent au plus ancien.
- [ ] Le filtre par statut ne renvoie que les candidatures de ce statut.
- [ ] Le tableau de bord renvoie le bon nombre de candidatures par statut, y compris 0 pour les statuts vides.
- [ ] Un candidat ne voit et ne modifie que ses propres candidatures.

### Limites

- **Le clic « J'ai postulé » reste manuel** : l'application ne détecte pas une candidature envoyée sur un site
  extérieur.
- **Pas de rappels ni de relances automatiques** dans cette version (piste : Scheduler Spring).
- **Pas de calendrier, de Kanban ni de statistiques avancées** : le tableau de bord se limite aux comptages par statut
  et aux dernières candidatures.
- **Statuts fixes** : le candidat ne peut pas en créer. La liste pourra évoluer selon les retours.
- **Dépendances** : cette feature s'appuie sur la table `offre` (feature *Recherche d'offres*) et sur l'entité
  `Utilisateur` avec l'authentification (Spring Security). À confirmer avec l'équipe.

## 2. Bilan (à compléter en fin de feature)

### Tests
<!-- Ce qui est testé, et comment. -->

### Usage de l'IA
<!-- Outils utilisés, pour quoi faire, ce qui a été repris, modifié ou rejeté. -->
