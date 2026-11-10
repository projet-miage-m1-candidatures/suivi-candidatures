# Guide de contribution

Comment on travaille ensemble sur ce projet : d'abord les règles pour les features, puis l'usage de Git et GitHub.

---

## 1. Une feature, une personne

Chaque membre porte **une feature majeure**, de bout en bout : base de données, back-end, API REST et interface.

## 2. Idéation : décrire la feature avant de coder

Avant d'écrire la moindre ligne de code, on décrit sa feature dans une **fiche** :

1. Copie le modèle `doc/features/MODELE-feature.md` et renomme-le, par exemple `doc/features/F1-adaptation-cv.md`.
2. Remplis la partie **Idéation** : le besoin, les scénarios d'usage, le fonctionnement, l'impact sur chaque couche,
   les critères d'acceptation et les limites.
3. Propose ta fiche dans une **pull request** (section 5). La relecture par un autre membre vaut validation :
   une fois la fiche fusionnée, tu peux commencer à coder.

La partie **Bilan** de la fiche se complète à la fin de la feature.

> Les étapes suivantes (base de données, back-end, tests, API, interface) seront précisées quand l'équipe aura
> choisi l'architecture.

---

## 3. Avant de commencer (une seule fois)

Accepte l'invitation à l'organisation reçue par email, puis :

```bash
git clone https://github.com/projet-miage-m1-candidatures/suivi-candidatures.git
cd suivi-candidatures
git config user.name "Prénom Nom"
git config user.email "email-de-ton-compte-github"
```

## 4. Les branches

- `main` : le travail de l'équipe. **On n'y écrit jamais directement.**
- `feature/nom-court` : **ta** branche, une par sujet. Exemple : `feature/fiche-adaptation-cv`.

## 5. Travailler avec Git, pas à pas

**1. Récupère la dernière version de `main`**

```bash
git switch main
git pull
```

**2. Crée ta branche**

```bash
git switch -c feature/fiche-adaptation-cv
```

**3. Travaille, puis enregistre ton travail par petits commits**

```bash
git status
git add <fichiers>
git commit -m "docs: ajoute la fiche de la feature adaptation du CV"
```

Commence le message par `feat:` (nouveauté), `fix:` (correction), `test:` (tests) ou `docs:` (documentation).

**4. Envoie ta branche sur GitHub**

```bash
git push -u origin feature/fiche-adaptation-cv
```

Au premier `push`, si Git te demande un mot de passe, GitHub ne l'acceptera pas : connecte ton compte GitHub
dans IntelliJ, ou installe [GitHub CLI](https://cli.github.com/) et lance `gh auth login`.

**5. Ouvre une pull request vers `main`**

- Dans la description : ce qui change, et comment le vérifier.
- Un autre membre relit. Toutes les remarques doivent être traitées.
- Si GitHub signale un conflit, clique sur **Update branch**, ou demande de l'aide.
- Fusion avec le bouton **Merge pull request**.


**6. Après la fusion, fais le ménage**
 
Sur GitHub, clique sur **Delete branch** dans la pull request : la branche est supprimée sur GitHub.
 
Puis, sur ton ordinateur :
 
```bash
git switch main
git pull
git branch -d feature/fiche-adaptation-cv
git fetch --prune
```
 
- `git switch main` : tu reviens sur `main` (on ne peut pas supprimer la branche sur laquelle on se trouve) ;
- `git pull` : tu récupères `main` à jour, avec ton travail fusionné ;
- `git branch -d …` : tu supprimes ta branche sur ton ordinateur ;
- `git fetch --prune` : tu effaces la trace de la branche supprimée sur GitHub.
**7. Recommence à l'étape 1** pour la suite.

---

## 6. Usage de l'IA

- L'IA est autorisée, mais **chacun doit comprendre et savoir expliquer** tout ce qu'il commite.
- Dans chaque pull request, indique en une ou deux lignes **si tu as utilisé l'IA et pour quoi faire**.

## 7. Les règles d'or

1. **Jamais de secret dans le dépôt** : pas de mot de passe, pas de clé d'API, pas de fichier `.env`.
   Le dépôt est public.
2. **Récupère `main` souvent**, pour éviter les conflits.
3. **Une branche = un sujet.** Ne mélange pas deux choses.
4. **En cas de doute, demande** sur le groupe.