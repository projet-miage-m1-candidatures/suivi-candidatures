// Outils partagés par « Mes candidatures » et le tableau de bord.
// Les fonctions sans DOM (analyserMarkdown, formaterDate…) sont testées dans src/test/js.

export const apiCandidatures = "/api/v1/candidatures";

export const statuts = {
	EN_ATTENTE: { libelle: "En attente", fond: "#e3edfc", texte: "#1d4fa8", point: "#7aa7ee" },
	ENTRETIEN: { libelle: "Entretien à préparer", fond: "#ece7fb", texte: "#4b3aa8", point: "#8f7be0" },
	REFUSEE: { libelle: "Refusée", fond: "#fde3e0", texte: "#8a2a22", point: "#d1493f" },
};

const formatCourt = new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "short" });
const formatLong = new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "short", year: "numeric" });
const formatJour = new Intl.DateTimeFormat("fr-FR", { weekday: "long", day: "numeric", month: "long" });

/** "2026-11-26" -> Date locale (sans décalage de fuseau). */
export function lireDate(texte) {
	if (!texte) return null;
	const [annee, mois, jour] = texte.split("-").map(Number);
	return new Date(annee, mois - 1, jour);
}

export function formaterDate(texte, style = "court") {
	const date = lireDate(texte);
	if (!date) return "";
	const format = { court: formatCourt, long: formatLong, jour: formatJour }[style] ?? formatCourt;
	return format.format(date);
}

/** Lit le message d'erreur JSON (ProblemDetail) renvoyé par Spring, sinon un message par statut. */
export async function messageErreur(reponse, parDefaut = "La demande a échoué. Réessayez dans un instant.") {
	try {
		const corps = await reponse.json();
		if (corps?.detail) return corps.detail;
	} catch {
		// corps vide ou non JSON
	}
	const messages = {
		400: "Vérifiez les informations saisies.",
		403: "La vérification de sécurité a échoué. Actualisez la page.",
		404: "Cette candidature est introuvable.",
		409: "Cette action n'est pas possible pour cette candidature.",
		502: "La génération a échoué. Réessayez dans un instant.",
		503: "L'assistant IA est indisponible pour le moment.",
	};
	return messages[reponse.status] ?? parDefaut;
}

/** Badge coloré d'un statut. */
export function creerBadge(statut) {
	const style = statuts[statut] ?? { libelle: statut, fond: "#e4e9f1", texte: "#35476b" };
	const badge = document.createElement("span");
	badge.textContent = style.libelle;
	badge.style.cssText = `padding:4px 12px;border-radius:999px;background:${style.fond};color:${style.texte};`
		+ "font-size:12.5px;font-weight:700;white-space:nowrap";
	return badge;
}

/** N'accepte que les liens http(s) : évite les liens « javascript: » saisis dans une offre. */
export function lienSur(url) {
	try {
		const lien = new URL(url);
		return lien.protocol === "http:" || lien.protocol === "https:" ? lien.href : null;
	} catch {
		return null;
	}
}

/**
 * Découpe le Markdown de l'IA en blocs simples (titres, paragraphes, listes).
 * On n'utilise jamais innerHTML : le texte vient de l'IA et de l'offre, il est affiché comme du texte.
 */
export function analyserMarkdown(texte) {
	const blocs = [];
	let paragraphe = [];
	let liste = null;

	const fermerParagraphe = () => {
		if (paragraphe.length) blocs.push({ type: "paragraphe", texte: paragraphe.join(" ") });
		paragraphe = [];
	};
	const fermerListe = () => {
		if (liste) blocs.push(liste);
		liste = null;
	};

	for (const ligneBrute of (texte ?? "").split(/\r?\n/)) {
		const ligne = ligneBrute.trim();
		let morceaux;
		if (!ligne) {
			fermerParagraphe();
			fermerListe();
		} else if ((morceaux = ligne.match(/^(#{1,6})\s+(.*)$/))) {
			fermerParagraphe();
			fermerListe();
			blocs.push({ type: "titre", niveau: morceaux[1].length, texte: morceaux[2] });
		} else if ((morceaux = ligne.match(/^[-*+]\s+(.*)$/)) || (morceaux = ligne.match(/^\d+[.)]\s+(.*)$/))) {
			fermerParagraphe();
			const ordonnee = /^\d/.test(ligne);
			if (!liste || liste.ordonnee !== ordonnee) {
				fermerListe();
				liste = { type: "liste", ordonnee, elements: [] };
			}
			liste.elements.push(morceaux[1]);
		} else if (/^(-{3,}|\*{3,})$/.test(ligne)) {
			fermerParagraphe();
			fermerListe();
		} else {
			fermerListe();
			paragraphe.push(ligne);
		}
	}
	fermerParagraphe();
	fermerListe();
	return blocs;
}

/** "texte **gras** suite" -> [{texte, gras:false}, {texte:"gras", gras:true}, …] */
export function analyserGras(texte) {
	return texte.split(/\*\*(.+?)\*\*/g)
		.map((morceau, index) => ({ texte: morceau, gras: index % 2 === 1 }))
		.filter(morceau => morceau.texte);
}

function remplirLigne(element, texte) {
	for (const morceau of analyserGras(texte)) {
		if (morceau.gras) {
			const fort = document.createElement("strong");
			fort.textContent = morceau.texte;
			element.append(fort);
		} else {
			element.append(document.createTextNode(morceau.texte));
		}
	}
}

export function afficherMarkdown(texte, conteneur) {
	conteneur.replaceChildren();
	for (const bloc of analyserMarkdown(texte)) {
		let element;
		if (bloc.type === "titre") {
			element = document.createElement(bloc.niveau <= 2 ? "h3" : "h4");
			element.style.cssText = bloc.niveau <= 2
				? "margin:22px 0 8px;font-size:20px;font-weight:700;color:#0f2a5c"
				: "margin:16px 0 6px;font-size:16.5px;font-weight:700;color:#1d4fa8";
			remplirLigne(element, bloc.texte);
		} else if (bloc.type === "liste") {
			element = document.createElement(bloc.ordonnee ? "ol" : "ul");
			element.style.cssText = "margin:0 0 10px;padding-left:22px;display:flex;flex-direction:column;gap:4px";
			for (const texteElement of bloc.elements) {
				const item = document.createElement("li");
				remplirLigne(item, texteElement);
				element.append(item);
			}
		} else {
			element = document.createElement("p");
			element.style.cssText = "margin:0 0 10px";
			remplirLigne(element, bloc.texte);
		}
		conteneur.append(element);
	}
}
