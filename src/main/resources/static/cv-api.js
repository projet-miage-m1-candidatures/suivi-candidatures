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
		contenu: {
			identite: { nom: "", titre: "", ville: "", email: "", telephone: "", lien: "", profil: "" },
			experiences: [],
			formations: [],
			competences: [],
			langues: [],
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

async function requeteCv(methode, chemin, corps) {
	const reponse = await appelerApi(methode, `${API_CV}${chemin}`, corps);
	if (!reponse.ok) {
		const probleme = await reponse.json().catch(() => null);
		throw new ErreurCv(reponse.status, probleme?.detail ?? `La requête sur les CV a échoué (${reponse.status}).`);
	}
	if (reponse.status !== 204) {
		return reponse.json();
	}
}
