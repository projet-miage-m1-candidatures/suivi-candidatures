// Page « Mes candidatures » : liste, filtres, ajout, changement de statut et préparation d'entretien par l'IA.
import { appelerApi } from "./api.js";
import {
	afficherMarkdown, apiCandidatures, creerBadge, formaterDate, lienSur, messageErreur, statuts,
} from "./candidatures-outils.js";

const etat = { filtre: "", candidatures: [], selection: null };

const $ = selecteur => document.querySelector(selecteur);
const liste = $("[data-liste-candidatures]");
const listeVide = $("[data-liste-vide]");
const erreurPage = $("[data-erreur-page]");
const boutonsFiltre = [...document.querySelectorAll("[data-filtre]")];

function afficherMessage(zone, message) {
	zone.textContent = message;
	zone.hidden = !message;
}

// ---------- Filtres et compteurs ----------

function styliserFiltres() {
	for (const bouton of boutonsFiltre) {
		const actif = bouton.dataset.filtre === etat.filtre;
		bouton.setAttribute("aria-pressed", String(actif));
		bouton.style.cssText = "min-height:44px;padding:0 18px;border-radius:999px;font-size:14.5px;font-weight:600;"
			+ "cursor:pointer;border:1.5px solid " + (actif ? "#0f2a5c;background:#0f2a5c;color:#fff"
				: "#bcd3f5;background:#fff;color:#0f2a5c");
	}
}

async function chargerCompteurs() {
	const reponse = await appelerApi("GET", `${apiCandidatures}/compteurs`);
	if (!reponse.ok) return;
	const compteurs = await reponse.json();
	const total = Object.values(compteurs).reduce((somme, n) => somme + n, 0);
	for (const bouton of boutonsFiltre) {
		const code = bouton.dataset.filtre;
		const libelle = code ? statuts[code].libelle : "Toutes";
		bouton.textContent = `${libelle} (${code ? compteurs[code] ?? 0 : total})`;
	}
}

// ---------- Liste ----------

function afficherListe() {
	liste.replaceChildren();
	listeVide.hidden = etat.candidatures.length > 0;
	etat.candidatures.forEach((candidature, index) => {
		const choisie = candidature.id === etat.selection?.id;
		const item = document.createElement("li");
		const bouton = document.createElement("button");
		bouton.type = "button";
		bouton.setAttribute("aria-current", String(choisie));
		bouton.style.cssText = "display:flex;flex-wrap:wrap;align-items:center;gap:8px 14px;width:100%;padding:14px 24px;"
			+ "border:0;text-align:left;cursor:pointer;color:#0f2a5c;font-family:inherit;"
			+ (index ? "border-top:1px solid #e3edfc;" : "")
			+ (choisie ? "background:#eaf2fe;box-shadow:inset 4px 0 0 #2a64cc" : "background:#fff");

		const texte = document.createElement("div");
		texte.style.cssText = "flex:1 1 200px;min-width:0";
		const titre = document.createElement("div");
		titre.style.cssText = "font-size:15.5px;font-weight:700;overflow-wrap:anywhere";
		titre.textContent = candidature.intitule;
		const sousTitre = document.createElement("div");
		sousTitre.style.cssText = "font-size:13.5px;color:#4a5f86;overflow-wrap:anywhere";
		sousTitre.textContent = [candidature.entreprise, candidature.lieu].filter(Boolean).join(" · ");
		texte.append(titre, sousTitre);

		const date = document.createElement("span");
		date.style.cssText = "width:62px;font-size:13.5px;color:#4a5f86";
		date.textContent = formaterDate(candidature.dateCandidature);

		bouton.append(texte, creerBadge(candidature.statut), date);
		bouton.addEventListener("click", () => selectionner(candidature));
		item.append(bouton);
		liste.append(item);
	});
}

async function chargerListe() {
	afficherMessage(erreurPage, "");
	const parametre = etat.filtre ? `?statut=${etat.filtre}` : "";
	try {
		const [reponse] = await Promise.all([appelerApi("GET", apiCandidatures + parametre), chargerCompteurs()]);
		if (reponse.status === 401) return;
		if (!reponse.ok) throw new Error(await messageErreur(reponse));
		etat.candidatures = await reponse.json();
		const selection = etat.candidatures.find(c => c.id === etat.selection?.id) ?? null;
		afficherListe();
		afficherDetail(selection);
	} catch (erreur) {
		afficherMessage(erreurPage, erreur.message || "Impossible de charger vos candidatures.");
	}
}

// ---------- Détail ----------

const detail = {
	vide: $("[data-detail-vide]"),
	contenu: $("[data-detail-contenu]"),
	statut: $("[data-detail-statut]"),
	intitule: $("[data-detail-intitule]"),
	entreprise: $("[data-detail-entreprise]"),
	dates: $("[data-detail-dates]"),
	lien: $("[data-detail-lien]"),
	formulaire: $("[data-formulaire-statut]"),
	select: $("#nouveau-statut"),
	champDate: $("[data-champ-date-entretien]"),
	date: $("#date-entretien"),
	erreurStatut: $("[data-erreur-statut]"),
	final: $("[data-statut-final]"),
	preparation: $("[data-bloc-preparation]"),
	generer: $("[data-generer-preparation]"),
	regenerer: $("[data-regenerer-preparation]"),
	etatPreparation: $("[data-etat-preparation]"),
	erreurPreparation: $("[data-erreur-preparation]"),
};
const fiche = {
	section: $("[data-fiche]"),
	titre: $("[data-fiche-titre]"),
	avertissement: $("[data-fiche-avertissement]"),
	contenu: $("[data-fiche-contenu]"),
	meta: $("[data-fiche-meta]"),
};

function selectionner(candidature) {
	if (etat.selection?.id !== candidature.id) fiche.section.hidden = true;
	history.replaceState(null, "", `#candidature-${candidature.id}`);
	afficherDetail(candidature);
	afficherListe();
}

function afficherDetail(candidature) {
	etat.selection = candidature;
	detail.vide.hidden = Boolean(candidature);
	detail.contenu.hidden = !candidature;
	if (!candidature) {
		fiche.section.hidden = true;
		return;
	}

	const style = statuts[candidature.statut];
	detail.statut.textContent = candidature.statutLibelle;
	detail.statut.style.background = style?.fond ?? "#e4e9f1";
	detail.statut.style.color = style?.texte ?? "#35476b";
	detail.intitule.textContent = candidature.intitule;
	detail.entreprise.textContent = [candidature.entreprise, candidature.lieu].filter(Boolean).join(" · ");
	detail.dates.textContent = `Candidature du ${formaterDate(candidature.dateCandidature, "long")}`
		+ (candidature.dateEntretien ? ` · Entretien le ${formaterDate(candidature.dateEntretien, "jour")}` : "");
	const lien = lienSur(candidature.urlOffre);
	detail.lien.hidden = !lien;
	if (lien) detail.lien.href = lien;

	const transitions = candidature.transitionsPossibles ?? [];
	detail.formulaire.hidden = transitions.length === 0;
	detail.final.hidden = transitions.length > 0;
	detail.select.replaceChildren(...transitions.map(code => new Option(statuts[code]?.libelle ?? code, code)));
	detail.date.value = "";
	majChampDate();
	afficherMessage(detail.erreurStatut, "");

	const entretien = candidature.statut === "ENTRETIEN";
	detail.preparation.hidden = !entretien;
	afficherMessage(detail.erreurPreparation, "");
	detail.etatPreparation.textContent = "";
	detail.regenerer.hidden = fiche.section.hidden;
}

function majChampDate() {
	detail.champDate.hidden = detail.select.value !== "ENTRETIEN";
}
detail.select.addEventListener("change", majChampDate);

detail.formulaire.addEventListener("submit", async evenement => {
	evenement.preventDefault();
	const bouton = detail.formulaire.querySelector("button[type=submit]");
	if (!etat.selection || bouton.disabled) return;
	bouton.disabled = true;
	afficherMessage(detail.erreurStatut, "");
	const corps = { statut: detail.select.value };
	if (corps.statut === "ENTRETIEN" && detail.date.value) corps.dateEntretien = detail.date.value;
	try {
		const reponse = await appelerApi("PATCH", `${apiCandidatures}/${etat.selection.id}/statut`, corps);
		if (reponse.status === 401) return;
		if (!reponse.ok) throw new Error(await messageErreur(reponse));
		etat.selection = await reponse.json();
		fiche.section.hidden = true;
		await chargerListe();
	} catch (erreur) {
		afficherMessage(detail.erreurStatut, erreur.message);
	} finally {
		bouton.disabled = false;
	}
});

// ---------- Préparation d'entretien ----------

async function preparer(regenerer) {
	if (!etat.selection) return;
	const candidature = etat.selection;
	detail.generer.disabled = detail.regenerer.disabled = true;
	afficherMessage(detail.erreurPreparation, "");
	detail.etatPreparation.textContent = "Génération en cours… cela peut prendre jusqu'à une minute.";
	try {
		const chemin = `${apiCandidatures}/${candidature.id}/preparation-entretien${regenerer ? "?regenerer=true" : ""}`;
		const reponse = await appelerApi("GET", chemin);
		if (reponse.status === 401) return;
		if (!reponse.ok) throw new Error(await messageErreur(reponse));
		const preparation = await reponse.json();
		if (etat.selection?.id !== candidature.id) return;
		fiche.titre.textContent = `Préparer l'entretien chez ${preparation.entreprise}`;
		fiche.avertissement.textContent = preparation.avertissement;
		afficherMarkdown(preparation.contenuMarkdown, fiche.contenu);
		const quand = new Date(preparation.genereeLe).toLocaleString("fr-FR", { dateStyle: "long", timeStyle: "short" });
		fiche.meta.textContent = `Générée le ${quand} · modèle ${preparation.modele}`;
		fiche.section.hidden = false;
		detail.regenerer.hidden = false;
		detail.etatPreparation.textContent = "Fiche prête, elle s'affiche sous la liste.";
		fiche.section.scrollIntoView({ behavior: "smooth", block: "start" });
	} catch (erreur) {
		detail.etatPreparation.textContent = "";
		afficherMessage(detail.erreurPreparation, erreur.message);
	} finally {
		detail.generer.disabled = detail.regenerer.disabled = false;
	}
}
detail.generer.addEventListener("click", () => preparer(false));
detail.regenerer.addEventListener("click", () => preparer(true));

// ---------- Ajout (« J'ai postulé ») ----------

const formulaireAjout = $("[data-formulaire-ajout]");
const boutonAjout = $("[data-ouvrir-ajout]");
const erreurAjout = $("[data-erreur-ajout]");

function ouvrirAjout(ouvert) {
	formulaireAjout.hidden = !ouvert;
	boutonAjout.setAttribute("aria-expanded", String(ouvert));
	if (ouvert) formulaireAjout.querySelector("input").focus();
}
boutonAjout.addEventListener("click", () => ouvrirAjout(formulaireAjout.hidden));
$("[data-annuler-ajout]").addEventListener("click", () => ouvrirAjout(false));

formulaireAjout.addEventListener("submit", async evenement => {
	evenement.preventDefault();
	const bouton = formulaireAjout.querySelector("button[type=submit]");
	if (bouton.disabled) return;
	bouton.disabled = true;
	afficherMessage(erreurAjout, "");
	const corps = Object.fromEntries(new FormData(formulaireAjout));
	try {
		const reponse = await appelerApi("POST", apiCandidatures, corps);
		if (reponse.status === 401) return;
		if (!reponse.ok) throw new Error(await messageErreur(reponse));
		const creee = await reponse.json();
		formulaireAjout.reset();
		ouvrirAjout(false);
		etat.filtre = "";
		etat.selection = creee;
		styliserFiltres();
		await chargerListe();
	} catch (erreur) {
		afficherMessage(erreurAjout, erreur.message);
	} finally {
		bouton.disabled = false;
	}
});

// ---------- Démarrage ----------

for (const bouton of boutonsFiltre) {
	bouton.addEventListener("click", () => {
		etat.filtre = bouton.dataset.filtre;
		styliserFiltres();
		chargerListe();
	});
}

async function demarrer() {
	styliserFiltres();
	const idDansAdresse = Number(location.hash.match(/^#candidature-(\d+)$/)?.[1]);
	if (idDansAdresse) etat.selection = { id: idDansAdresse };
	await chargerListe();
}
demarrer();
