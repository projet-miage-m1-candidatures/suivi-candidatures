// Affichage d'un CV avec son modèle et sa police, et calcul de sa complétude.
// Partagé par « Mes CV » (miniatures) et « Créer un CV » (aperçu).

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

const TITRES = {
	FR: { profil: "Profil", experiences: "Expériences", formations: "Formation", competences: "Compétences", langues: "Langues", nom: "Votre nom" },
	EN: { profil: "Profile", experiences: "Experience", formations: "Education", competences: "Skills", langues: "Languages", nom: "Your name" },
};

export function echapper(texte) {
	return String(texte ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}

export function periode(debut, fin) {
	return [debut, fin].map(t => (t ?? "").trim()).filter(Boolean).join(" – ");
}

function rendreContenu(cv) {
	const c = cv.contenu;
	const id = c.identite;
	const t = TITRES[cv.langue] ?? TITRES.FR;
	const contact = [id.ville, id.email, id.telephone, id.lien].filter(v => v?.trim()).map(echapper).join(" · ");
	const section = (titre, corps) => corps ? `<section class="cv-section"><h2>${titre}</h2><div class="cv-corps">${corps}</div></section>` : "";
	const puces = liste => {
		const lignes = (liste ?? []).map(l => l.trim()).filter(Boolean);
		return lignes.length ? `<ul>${lignes.map(l => `<li>${echapper(l)}</li>`).join("")}</ul>` : "";
	};
	const date = (debut, fin) => {
		const p = periode(debut, fin);
		return p ? `<span class="cv-date">${echapper(p)}</span>` : "";
	};

	const experiences = c.experiences.filter(e => e.poste?.trim() || e.entreprise?.trim()).map(e =>
		`<div class="cv-item"><p>${date(e.debut, e.fin)}<strong>${echapper(e.poste)}</strong>${e.entreprise?.trim() ? " · " + echapper(e.entreprise) : ""}${e.lieu?.trim() ? `<span class="cv-sous"> · ${echapper(e.lieu)}</span>` : ""}</p>${puces(e.realisations)}</div>`).join("");
	const formations = c.formations.filter(f => f.diplome?.trim()).map(f =>
		`<div class="cv-item"><p>${date(f.debut, f.fin)}<strong>${echapper(f.diplome)}</strong>${f.ecole?.trim() ? " · " + echapper(f.ecole) : ""}</p></div>`).join("");
	const competences = c.competences.filter(g => g.elements.length).map(g =>
		`<p>${g.groupe?.trim() ? `<strong>${echapper(g.groupe)} :</strong> ` : ""}${g.elements.map(echapper).join(", ")}</p>`).join("");
	const langues = c.langues.filter(l => l.langue?.trim()).map(l => echapper(l.langue) + (l.niveau?.trim() ? ` (${echapper(l.niveau)})` : "")).join(" · ");

	const nom = id.nom?.trim() ? echapper(id.nom) : `<span class="cv-vide">${t.nom}</span>`;
	return `<header class="cv-tete"><h1 class="cv-nom">${nom}</h1>${id.titre?.trim() ? `<p class="cv-titre">${echapper(id.titre)}</p>` : ""}${contact ? `<p class="cv-contact">${contact}</p>` : ""}</header>`
		+ section(t.profil, id.profil?.trim() ? `<p>${echapper(id.profil)}</p>` : "")
		+ section(t.experiences, experiences)
		+ section(t.formations, formations)
		+ section(t.competences, competences)
		+ section(t.langues, langues ? `<p>${langues}</p>` : "");
}

// Remplit un élément <article> avec le CV, dans son modèle et sa police.
export function afficherCv(article, cv) {
	article.className = `cv modele-${(cv.modele ?? "CLASSIQUE").toLowerCase()}`;
	article.style.setProperty("--cv-police", (POLICES[cv.police] ?? POLICES.INTER).css);
	article.lang = cv.langue === "EN" ? "en" : "fr";
	article.innerHTML = rendreContenu(cv);
}

// Complétude du CV : des règles simples, sans IA. Chaque point à compléter renvoie à son étape du formulaire.
export function evaluerCv(contenu) {
	const regles = [];
	const ajouter = (poids, ok, conseil, etape) => regles.push({ poids, ok, conseil, etape });
	const id = contenu.identite;
	const experiences = contenu.experiences.filter(e => e.poste?.trim());
	const realisations = experiences.flatMap(e => e.realisations.filter(r => r.trim()));
	const nbCompetences = contenu.competences.reduce((n, g) => n + g.elements.length, 0);

	ajouter(20, Boolean(id.nom?.trim() && id.titre?.trim() && id.email?.trim()), "Complétez l'identité : nom, poste visé et e-mail.", 0);
	ajouter(10, Boolean(id.profil?.trim()), "Ajoutez un profil de deux ou trois lignes en haut du CV.", 0);
	ajouter(20, experiences.length > 0, "Ajoutez au moins une expérience (stage, emploi, projet).", 1);
	const peuDetaillee = experiences.find(e => e.realisations.filter(r => r.trim()).length < 2);
	ajouter(10, experiences.length > 0 && !peuDetaillee, peuDetaillee
		? `Ajoutez au moins deux réalisations à « ${peuDetaillee.poste.trim()} ».`
		: "Décrivez chaque expérience avec au moins deux réalisations.", 1);
	ajouter(10, realisations.some(r => /\d/.test(r)), "Ajoutez un résultat chiffré (pourcentage, volume, durée) dans une réalisation.", 1);
	ajouter(10, contenu.formations.some(f => f.diplome?.trim()), "Ajoutez votre formation.", 2);
	ajouter(10, nbCompetences >= 5, "Listez au moins cinq compétences, regroupées par thème.", 3);
	ajouter(10, contenu.langues.some(l => l.langue?.trim()), "Indiquez au moins une langue et son niveau.", 4);

	return {
		score: regles.filter(r => r.ok).reduce((s, r) => s + r.poids, 0),
		conseils: regles.filter(r => !r.ok).map(r => r.conseil),
		aCompleter: regles.filter(r => !r.ok).map(r => ({ texte: r.conseil, etape: r.etape })),
	};
}
