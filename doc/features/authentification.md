# Authentification

| | |
|---|---|
| Responsable | Kyrylo NEBYLYTSIA (`@NebylytsiaKyrylo`) |
| Version visée | v0.2.0 |
| Statut | Idéation |

## 1. Idéation

### Besoin

Un candidat doit pouvoir retrouver ses offres, ses CV et ses candidatures à chaque visite. L'application
doit identifier le compte qui appelle l'API pour que chaque fonctionnalité puisse limiter l'accès aux données
de ce compte.

### Scénarios d'usage

- *En tant que* candidat, je crée un compte avec mon prénom, mon nom, mon e-mail et mon mot de passe **afin de**
  commencer à utiliser l'application.
- *En tant que* candidat, je me connecte avec mon e-mail et mon mot de passe **afin de** retrouver mes données.
- *En tant que* candidat, je me déconnecte **afin de** fermer ma session, par exemple sur un ordinateur partagé.

### Fonctionnement

1. **Inscription.** Le serveur valide les champs, retire les espaces autour de l'e-mail et le passe en minuscules.
   Le mot de passe doit contenir au moins 15 caractères. Le serveur le hache avec BCrypt et crée le compte dans
   PostgreSQL. Une contrainte `UNIQUE` sur l'e-mail normalisé empêche les doublons, y compris lors de deux
   inscriptions simultanées. Après la réponse `201`, le JavaScript ouvre `connexion.html` pour que le candidat
   se connecte.
2. **Connexion.** Le navigateur envoie l'e-mail et le mot de passe sous forme de champs de formulaire avec `fetch`.
   La configuration de `formLogin` associe `usernameParameter` à `email` et `passwordParameter` à `motDePasse`.
   Le serveur normalise l'e-mail comme à l'inscription et vérifie le mot de passe à partir du hachage.
   En cas de succès, Spring Security conserve l'identité du compte dans la session côté serveur.
   Le navigateur reçoit `JSESSIONID`, le cookie qui contient l'identifiant de la session.
3. **Requêtes suivantes.** Le navigateur joint `JSESSIONID` aux appels vers l'application. Spring Security retrouve
   la session. Le service `CandidatCourant` donne aux modules métier l'identifiant du compte connecté. Chaque module
   utilise cet identifiant pour vérifier que les données demandées appartiennent au candidat.
4. **Déconnexion.** Le navigateur envoie un `POST` et Spring Security invalide la session. Une requête ultérieure
   en `GET` vers une route protégée avec l'ancien identifiant de session reçoit `401`.

**Protection CSRF.** Le navigateur joint automatiquement le cookie de session. Pour protéger les requêtes qui
modifient un état, Spring Security vérifie aussi un jeton CSRF. Avant le premier `POST`, le JavaScript commun
appelle la route publique `GET /api/v1/auth/csrf`. Le serveur rend le jeton disponible dans le cookie
`XSRF-TOKEN`. Pour chaque `POST`, le JavaScript lit ce cookie et recopie sa valeur dans l'en-tête
`X-XSRF-TOKEN`, y compris pour l'inscription, la connexion et la déconnexion. Après une connexion ou une
déconnexion réussie, il rappelle la route publique pour obtenir un jeton frais avant le prochain `POST`.
[Intégration du CSRF avec JavaScript dans Spring Security](https://docs.spring.io/spring-security/reference/7.1/servlet/exploits/csrf.html#csrf-integration-javascript)

### Impact sur les couches

- **Base de données.** Une migration Flyway `V<date>_<heure>__description.sql` crée la table `utilisateur` avec un
  identifiant, un e-mail unique, le hachage du mot de passe, le prénom et le nom.
- **Back-end.** L'entité `Utilisateur` représente le compte et son repository permet de le retrouver par e-mail.
  Le service d'inscription valide et crée le compte. Spring Security gère la connexion et la déconnexion.
  `commun.CandidatCourant` expose l'identifiant du candidat aux modules `cv`, `offres` et `candidatures`.
- **API.** Le contrat proposé pour l'interface est le suivant.

  | Route | Entrée | Réponse attendue |
  |---|---|---|
  | `GET /api/v1/auth/csrf` | Accès public | `204`, jeton disponible dans le cookie `XSRF-TOKEN` |
  | `POST /api/v1/auth/inscription` | Corps JSON (`application/json`) avec `prenom`, `nom`, `email`, `motDePasse` | `201` si le compte est créé, `400` si les champs sont invalides, `409` si l'e-mail est déjà utilisé |
  | `POST /api/v1/auth/connexion` | Champs `email` et `motDePasse` encodés en `application/x-www-form-urlencoded` | `200` si la connexion réussit, `401` si les identifiants sont invalides |
  | `GET /api/v1/auth/moi` | Cookie de session | `200` avec un objet JSON contenant uniquement `id`, `email`, `prenom` et `nom`, ou `401` si la session est absente ou expirée |
  | `POST /api/v1/auth/deconnexion` | Cookie de session | `204` après invalidation de la session |

  L'inscription et la connexion sont accessibles avant authentification. Les routes protégées sous `/api/**`
  renvoient `401` en l'absence de session authentifiée, sans redirection. Un jeton CSRF manquant ou invalide peut
  entraîner un `403` avant le contrôle de la session. `/actuator/health` reste public.
- **Interface.** Relier la maquette `connexion.html` au serveur, en retirant les valeurs de démonstration. Ajouter
  `inscription.html` et brancher la déconnexion sur l'interface existante. Les deux pages et l'accueil restent publics.
  Les pages qui affichent l'identité du candidat appellent `GET /api/v1/auth/moi` au chargement. Le tableau
  de bord utilise le prénom reçu pour afficher « Bonjour, Alex », par exemple. Sur un `401`, il ouvre `connexion.html`.

### Critères d'acceptation

- [ ] Une inscription valide crée un compte. PostgreSQL stocke le hachage BCrypt et l'API ne le renvoie jamais.
- [ ] À données valides par ailleurs, un mot de passe de 14 caractères entraîne un `400` et aucun compte n'est créé.
  Avec 15 caractères, l'inscription réussit et le navigateur affiche la page de connexion.
- [ ] Deux adresses qui deviennent identiques après normalisation ne créent qu'un seul compte. La seconde
  inscription reçoit `409`, ce qui indique que l'adresse est déjà utilisée.
- [ ] Des identifiants valides permettent d'utiliser une route protégée dans une deuxième requête avec la même session.
- [ ] Des identifiants invalides déclenchent un `401` avec le même message pour un e-mail inconnu et un mauvais mot de passe.
- [ ] L'accueil, les pages de connexion et d'inscription et `/actuator/health` répondent sans session authentifiée.
- [ ] Une route protégée appelée en `GET` sans session renvoie `401`, sans redirection.
- [ ] Une fois Alex connecté, `GET /api/v1/auth/moi` renvoie ses quatre champs et le tableau de bord affiche
  « Bonjour, Alex ». Si sa session expire, la même route renvoie `401` et le tableau de bord ouvre la page de connexion.
- [ ] Avant la connexion, un appel à `GET /api/v1/auth/csrf` fournit le cookie nécessaire à l'envoi du
  formulaire. Après connexion, le jeton renouvelé permet de se déconnecter. Après déconnexion, le navigateur
  peut obtenir un nouveau jeton pour se reconnecter.
- [ ] Une inscription, une connexion ou une déconnexion envoyée sans jeton CSRF valide renvoie `403`.
- [ ] Après déconnexion, l'ancien cookie ne permet plus d'accéder à une route protégée.
- [ ] `CandidatCourant` fournit l'identifiant du compte associé à la session de la requête.

### Limites

La première version couvre la création de compte, la connexion et la déconnexion. La vérification d'e-mail,
la réinitialisation du mot de passe et l'option « Rester connecté » sont hors périmètre. Les mots de passe,
les clés et le fichier `.env` restent hors du dépôt public.

### Ordre de réalisation

1. Créer la migration, l'entité `Utilisateur` et le repository. Vérifier le tout avec PostgreSQL.
2. Configurer Spring Security, la route publique du jeton CSRF et l'inscription. Tester le hachage,
   la validation et les doublons.
3. Brancher la connexion, la déconnexion, la route du profil connecté et les pages. Tester la session sur
   plusieurs requêtes et le renouvellement du jeton CSRF.
4. Ajouter `CandidatCourant` et tester l'identifiant renvoyé pour un utilisateur connecté.

Chaque étape est validée avec `./gradlew build` avant de passer à la suivante.

Pour les tests d'intégration des modules métier, prévoir une fonction commune dans les sources de test.
Elle crée un candidat dans la base de test, le connecte et fournit au test son identifiant et sa session.
Les tests des offres, des CV et des candidatures peuvent ainsi appeler leurs routes au nom de ce candidat.

## 2. Bilan (à compléter en fin de feature)

### Tests

<!-- Ce qui est testé, et comment. -->

### Usage de l'IA

<!-- Outils utilisés, pour quoi faire, ce qui a été repris, modifié ou rejeté. -->
