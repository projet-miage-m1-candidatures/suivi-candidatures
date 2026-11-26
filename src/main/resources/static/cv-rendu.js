// Affichage d'un CV avec son modèle et sa police, dates, et complétude.
// Partagé par « Mes CV » (miniatures) et « Créer un CV » (aperçu et vignettes des modèles).

export const MODELES = {
	CLASSIQUE: { nom: "Classique", police: "SOURCE_SERIF_4", description: "En-tête centré, titres en petites capitales soulignées." },
	MODERNE: { nom: "Moderne", police: "INTER", description: "Bandeau de couleur à gauche de l'en-tête, titres en bleu." },
	COMPACT: { nom: "Compact", police: "IBM_PLEX_SANS", description: "Dense, titres dans la marge, pensé pour tenir sur une page." },
};

export const POLICES = {
	INTER: { nom: "Inter", css: "'Inter', Arial, sans-serif" },
	LATO: { nom: "Lato", css: "'Lato', Arial, sans-serif" },
	IBM_PLEX_SANS: { nom: "IBM Plex Sans", css: "'IBM Plex Sans', Arial, sans-serif" },
	SOURCE_SERIF_4: { nom: "Source Serif 4", css: "'Source Serif 4', Georgia, serif" },
	LORA: { nom: "Lora", css: "'Lora', Georgia, serif" },
};

export const PERMIS = ["AM", "A1", "A2", "A", "B", "BE", "C1", "C1E", "C", "CE", "D1", "D1E", "D", "DE"];

const TEXTES = {
	FR: {
		profil: "Profil", experiences: "Expériences", formations: "Formation", projets: "Projets",
		competences: "Compétences", savoirEtre: "Savoir-être", langues: "Langues", certifications: "Certifications",
		centresInteret: "Centres d'intérêt", nom: "Votre nom", aujourdhui: "aujourd'hui", permis: "Permis",
		immediat: "Disponible immédiatement", aPartirDu: "Disponible à partir du", mention: "Mention",
		mois: ["janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."],
		moisLongs: ["janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"],
	},
	EN: {
		profil: "Profile", experiences: "Experience", formations: "Education", projets: "Projects",
		competences: "Skills", savoirEtre: "Soft skills", langues: "Languages", certifications: "Certifications",
		centresInteret: "Interests", nom: "Your name", aujourdhui: "Present", permis: "Driving licence",
		immediat: "Available immediately", aPartirDu: "Available from", mention: "Honours",
		mois: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
		moisLongs: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
	},
};

export function echapper(texte) {
	return String(texte ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}

const rempli = valeur => typeof valeur === "string" ? valeur.trim() !== "" : Boolean(valeur);

// Complète un contenu (ancien format compris) pour que toutes les listes et tous les champs existent.
export function normaliserContenu(contenu = {}) {
	const ancien = contenu.identite ?? {};
	const identite = {
		prenom: "", nom: "", titre: "", ville: "", email: "", telephone: "", permis: [], liens: [],
		...ancien,
	};
	if (!Array.isArray(identite.permis)) identite.permis = [];
	if (!Array.isArray(identite.liens)) identite.liens = [];
	if (rempli(ancien.lien) && identite.liens.length === 0) identite.liens = [{ libelle: "Lien", url: ancien.lien }];
	delete identite.lien;
	const profil = contenu.profil ?? ancien.profil ?? "";
	delete identite.profil;
	const liste = cle => Array.isArray(contenu[cle]) ? contenu[cle] : [];
	return {
		identite,
		profil,
		competences: liste("competences").map(g => ({ groupe: "", ...g, elements: Array.isArray(g.elements) ? g.elements : [] })),
		savoirEtre: liste("savoirEtre"),
		centresInteret: liste("centresInteret"),
		experiences: liste("experiences").map(e => ({ poste: "", entreprise: "", lieu: "", debut: "", fin: "", enCours: false, description: "", ...e, realisations: Array.isArray(e.realisations) ? e.realisations : [] })),
		formations: liste("formations").map(({ ecole, ...f }) => ({ diplome: "", etablissement: ecole ?? "", mention: "", debut: "", fin: "", enCours: false, description: "", ...f })),
		projets: liste("projets").map(p => ({ titre: "", lien: "", description: "", ...p })),
		langues: liste("langues").map(l => ({ langue: "", niveau: "", ...l })),
		certifications: liste("certifications").map(c => ({ nom: "", organisme: "", date: "", lien: "", ...c })),
		disponibilite: { immediate: false, date: "", ...(contenu.disponibilite ?? {}) },
	};
}

// Une date est enregistrée « AAAA » ou « AAAA-MM » ; tout autre texte (ancien format) est affiché tel quel.
export function formaterDate(valeur, langue = "FR") {
	const texte = String(valeur ?? "").trim();
	const m = texte.match(/^(\d{4})(?:-(\d{2}))?$/);
	if (!m) return texte;
	const mois = m[2] ? TEXTES[langue]?.mois[Number(m[2]) - 1] ?? TEXTES.FR.mois[Number(m[2]) - 1] : "";
	return mois ? `${mois} ${m[1]}` : m[1];
}

export function formaterJour(valeur, langue = "FR") {
	const m = String(valeur ?? "").match(/^(\d{4})-(\d{2})-(\d{2})$/);
	if (!m) return String(valeur ?? "");
	const t = TEXTES[langue] ?? TEXTES.FR;
	const jour = Number(m[3]);
	const mois = t.moisLongs[Number(m[2]) - 1];
	return langue === "EN" ? `${mois} ${jour}, ${m[1]}` : `${jour === 1 ? "1er" : jour} ${mois} ${m[1]}`;
}

// « ouverte » : une période avec seulement un début se termine « aujourd'hui » (expériences).
export function periode({ debut, fin, enCours }, langue = "FR", ouverte = false) {
	const t = TEXTES[langue] ?? TEXTES.FR;
	const d = formaterDate(debut, langue);
	const f = formaterDate(fin, langue);
	if (d && (enCours || (ouverte && !f))) return `${d} – ${t.aujourdhui}`;
	return [d, f].filter(Boolean).join(" – ");
}

// Lien cliquable : seules les adresses web sont acceptées ; « github.com/x » devient « https://github.com/x ».
// Un autre schéma (javascript:, mailto:…) ne donne pas de lien, comme dans le PDF (CvHtml.adresseWeb).
export function adresseWeb(valeur) {
	const texte = String(valeur ?? "").trim();
	if (!texte) return "";
	if (/^https?:\/\//i.test(texte)) return texte;
	return /^[a-z][a-z0-9+.-]*:/i.test(texte) ? "" : `https://${texte}`;
}

export function nomComplet(identite) {
	return [identite.prenom, identite.nom].map(v => (v ?? "").trim()).filter(Boolean).join(" ");
}

function rendreContenu(cv) {
	const c = normaliserContenu(cv.contenu);
	const id = c.identite;
	const langue = cv.langue === "EN" ? "EN" : "FR";
	const t = TEXTES[langue];
	const section = (titre, corps) => corps ? `<section class="cv-section"><h2>${titre}</h2><div class="cv-corps">${corps}</div></section>` : "";
	const puces = liste => {
		const lignes = (liste ?? []).map(l => String(l).trim()).filter(Boolean);
		return lignes.length ? `<ul>${lignes.map(l => `<li>${echapper(l)}</li>`).join("")}</ul>` : "";
	};
	const date = texte => texte ? `<span class="cv-date">${echapper(texte)}</span>` : "";
	const lien = (texte, url) => rempli(url) ? `<a href="${echapper(adresseWeb(url))}" target="_blank" rel="noopener">${texte}</a>` : texte;
	const paragraphe = texte => rempli(texte) ? `<p class="cv-description">${echapper(texte)}</p>` : "";

	const contact = [
		...[id.ville, id.email, id.telephone].filter(rempli).map(echapper),
		...id.liens.filter(l => adresseWeb(l.url)).map(l => lien(echapper(l.url.trim()), l.url)),
	].join(" · ");
	const complements = [
		id.permis.length ? `${t.permis} ${id.permis.map(echapper).join(", ")}` : "",
		c.disponibilite.immediate ? t.immediat : rempli(c.disponibilite.date) ? `${t.aPartirDu} ${echapper(formaterJour(c.disponibilite.date, langue))}` : "",
	].filter(Boolean).join(" · ");

	const experiences = c.experiences.filter(e => rempli(e.poste) || rempli(e.entreprise)).map(e =>
		`<div class="cv-item"><p>${date(periode(e, langue, true))}<strong>${echapper(e.poste)}</strong>${rempli(e.entreprise) ? " · " + echapper(e.entreprise) : ""}${rempli(e.lieu) ? `<span class="cv-sous"> · ${echapper(e.lieu)}</span>` : ""}</p>${paragraphe(e.description)}${puces(e.realisations)}</div>`).join("");
	const formations = c.formations.filter(f => rempli(f.diplome)).map(f =>
		`<div class="cv-item"><p>${date(periode(f, langue))}<strong>${echapper(f.diplome)}</strong>${rempli(f.etablissement) ? " · " + echapper(f.etablissement) : ""}</p>${rempli(f.mention) ? `<p class="cv-sous">${t.mention} ${echapper(f.mention)}</p>` : ""}${paragraphe(f.description)}</div>`).join("");
	const projets = c.projets.filter(p => rempli(p.titre)).map(p =>
		`<div class="cv-item"><p><strong>${lien(echapper(p.titre), p.lien)}</strong></p>${paragraphe(p.description)}</div>`).join("");
	const competences = c.competences.filter(g => g.elements.length).map(g =>
		`<p>${rempli(g.groupe) ? `<strong>${echapper(g.groupe)} :</strong> ` : ""}${g.elements.map(echapper).join(", ")}</p>`).join("")
		+ (c.savoirEtre.filter(rempli).length ? `<p><strong>${t.savoirEtre} :</strong> ${c.savoirEtre.filter(rempli).map(echapper).join(", ")}</p>` : "");
	const langues = c.langues.filter(l => rempli(l.langue)).map(l => echapper(l.langue) + (rempli(l.niveau) ? ` (${echapper(l.niveau)})` : "")).join(" · ");
	const certifications = c.certifications.filter(x => rempli(x.nom)).map(x =>
		`<div class="cv-item"><p>${date(formaterDate(x.date, langue))}<strong>${lien(echapper(x.nom), x.lien)}</strong>${rempli(x.organisme) ? " · " + echapper(x.organisme) : ""}</p></div>`).join("");
	const interets = c.centresInteret.filter(rempli).map(echapper).join(" · ");

	const nom = nomComplet(id) ? echapper(nomComplet(id)) : `<span class="cv-vide">${t.nom}</span>`;
	return `<header class="cv-tete"><h1 class="cv-nom">${nom}</h1>${rempli(id.titre) ? `<p class="cv-titre">${echapper(id.titre)}</p>` : ""}${contact ? `<p class="cv-contact">${contact}</p>` : ""}${complements ? `<p class="cv-contact">${complements}</p>` : ""}</header>`
		// Ordre des sections = ordre des étapes du formulaire (ETAPES).
		+ section(t.profil, rempli(c.profil) ? `<p>${echapper(c.profil)}</p>` : "")
		+ section(t.competences, competences)
		+ section(t.experiences, experiences)
		+ section(t.projets, projets)
		+ section(t.formations, formations)
		+ section(t.certifications, certifications)
		+ section(t.langues, langues ? `<p>${langues}</p>` : "")
		+ section(t.centresInteret, interets ? `<p>${interets}</p>` : "");
}

// Remplit un élément <article> avec le CV, dans son modèle et sa police.
export function afficherCv(article, cv) {
	article.className = `cv modele-${(cv.modele ?? "CLASSIQUE").toLowerCase()}`;
	article.style.setProperty("--cv-police", (POLICES[cv.police] ?? POLICES.INTER).css);
	article.lang = cv.langue === "EN" ? "en" : "fr";
	article.innerHTML = rendreContenu(cv);
}

// Étapes du formulaire, dans l'ordre des sections du CV (indices utilisés par evaluerCv et sectionRemplie).
// « Profil » regroupe l'en-tête du CV (identité, coordonnées, permis, disponibilité) et le résumé.
export const ETAPES = ["Profil", "Compétences", "Expériences", "Projets", "Formation", "Certifications", "Langues", "Vérification"];

// Une section est « remplie » dès qu'elle contient l'essentiel ; les conseils restent dans Vérification.
export function sectionRemplie(contenuBrut, etape) {
	const c = normaliserContenu(contenuBrut);
	switch (etape) {
		case 0: return rempli(c.identite.prenom) && rempli(c.identite.nom);
		case 1: return c.competences.some(g => g.elements.length) || c.savoirEtre.some(rempli);
		case 2: return c.experiences.some(e => rempli(e.poste));
		case 3: return c.projets.some(p => rempli(p.titre));
		case 4: return c.formations.some(f => rempli(f.diplome));
		case 5: return c.certifications.some(x => rempli(x.nom));
		case 6: return c.langues.some(l => rempli(l.langue));
		default: return evaluerCv(contenuBrut).aCompleter.length === 0;
	}
}

// Complétude du CV : des règles simples, sans IA. Chaque point à compléter renvoie à son étape du formulaire.
export function evaluerCv(contenuBrut) {
	const c = normaliserContenu(contenuBrut);
	const regles = [];
	const ajouter = (poids, ok, conseil, etape) => regles.push({ poids, ok, conseil, etape });
	const id = c.identite;
	const experiences = c.experiences.filter(e => rempli(e.poste));
	const textes = experiences.flatMap(e => [e.description, ...e.realisations]).filter(rempli);
	const nbCompetences = c.competences.reduce((n, g) => n + g.elements.length, 0);

	ajouter(15, rempli(id.prenom) && rempli(id.nom) && rempli(id.titre) && rempli(id.email), "Complétez l'identité : prénom, nom, poste visé et e-mail.", 0);
	ajouter(10, rempli(c.profil), "Ajoutez un résumé de deux ou trois lignes en haut du CV.", 0);
	ajouter(10, nbCompetences >= 3, "Listez au moins trois compétences techniques.", 1);
	ajouter(15, experiences.length > 0, "Ajoutez au moins une expérience (stage, emploi, alternance).", 2);
	// Une expérience est détaillée avec une description ou au moins deux réalisations.
	const peuDetaillee = experiences.find(e => !rempli(e.description) && e.realisations.filter(rempli).length < 2);
	ajouter(10, experiences.length > 0 && !peuDetaillee, peuDetaillee
		? `Décrivez « ${peuDetaillee.poste.trim()} » : une description ou au moins deux réalisations.`
		: "Décrivez chaque expérience : une description ou au moins deux réalisations.", 2);
	ajouter(10, textes.some(r => /\d/.test(r)), "Ajoutez un résultat chiffré (pourcentage, volume, durée) dans une expérience.", 2);
	ajouter(10, c.formations.some(f => rempli(f.diplome)), "Ajoutez votre formation.", 4);
	ajouter(10, c.langues.some(l => rempli(l.langue)), "Indiquez au moins une langue et son niveau.", 6);
	ajouter(10, c.disponibilite.immediate || rempli(c.disponibilite.date), "Indiquez votre disponibilité.", 0);

	return {
		score: regles.filter(r => r.ok).reduce((s, r) => s + r.poids, 0),
		conseils: regles.filter(r => !r.ok).map(r => r.conseil),
		aCompleter: regles.filter(r => !r.ok).map(r => ({ texte: r.conseil, etape: r.etape })),
	};
}
