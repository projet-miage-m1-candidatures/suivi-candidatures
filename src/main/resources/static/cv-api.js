import { appelerApi } from "./api.js";

const API_CV = "/api/v1/cv";

export class ErreurCv extends Error {
	constructor(statut, message) {
		super(message);
		this.statut = statut;
	}
}

export function nouveauCv() {
	return {
		titre: "",
		langue: "FR",
		modele: "CLASSIQUE",
		police: "SOURCE_SERIF_4",
		couleur: "NOIR",
		taille: "NORMALE",
		contenu: {
			identite: { prenom: "", nom: "", titre: "", ville: "", email: "", telephone: "", mobilite: "", permis: [], liens: [] },
			profil: "",
			competences: [],
			savoirEtre: [],
			centresInteret: [],
			experiences: [],
			formations: [],
			projets: [],
			langues: [],
			certifications: [],
			disponibilite: { immediate: false, date: "" },
		},
	};
}

export async function listerCv() {
	return requeteCv("GET", "");
}

export async function lireCv(id) {
	return requeteCv("GET", `/${encodeURIComponent(id)}`);
}

export async function creerCv(cv) {
	return requeteCv("POST", "", cv);
}

export async function enregistrerCv(cv) {
	return requeteCv("PUT", `/${encodeURIComponent(cv.id)}`, cv);
}

export async function supprimerCv(id) {
	return requeteCv("DELETE", `/${encodeURIComponent(id)}`);
}

export async function dupliquerCv(id) {
	return requeteCv("POST", `/${encodeURIComponent(id)}/copie`);
}

export async function telechargerPdf(id) {
	const reponse = await appelerCv("GET", `/${encodeURIComponent(id)}/pdf`);
	const disposition = reponse.headers.get("Content-Disposition");
	const nom = disposition?.match(/filename="([^"]+)"/i)?.[1] ?? "CV.pdf";
	const url = URL.createObjectURL(await reponse.blob());
	const lien = document.createElement("a");
	lien.href = url;
	lien.download = nom;
	document.body.append(lien);
	lien.click();
	lien.remove();
	setTimeout(() => URL.revokeObjectURL(url), 1000);
}

async function requeteCv(methode, chemin, corps) {
	const reponse = await appelerCv(methode, chemin, corps);
	if (reponse.status !== 204) {
		return reponse.json();
	}
}

async function appelerCv(methode, chemin, corps) {
	const reponse = await appelerApi(methode, `${API_CV}${chemin}`, corps);
	if (!reponse.ok) {
		const probleme = await reponse.json().catch(() => null);
		throw new ErreurCv(reponse.status, probleme?.detail ?? `La requête sur les CV a échoué (${reponse.status}).`);
	}
	return reponse;
}
