import assert from "node:assert/strict";
import { test } from "node:test";

import { ETAPES, evaluerCv, sectionRemplie } from "../../main/resources/static/cv-rendu.js";

const SECTIONS = ETAPES.length - 1; // toutes les étapes sauf « Vérification »

const complet = () => ({
	identite: { prenom: "Alex", nom: "Moreau", titre: "Data engineer", email: "alex.moreau@exemple.fr" },
	profil: "Étudiant en M1 MIAGE, je conçois des pipelines de données.",
	competences: [{ groupe: "Langages", elements: ["SQL", "Python", "Java"] }],
	experiences: [{ poste: "Stagiaire data", entreprise: "Nordlys", realisations: ["Réduit de 40 % le temps de chargement", "Tests de qualité"] }],
	projets: [{ titre: "Pipeline de données" }],
	formations: [{ diplome: "M1 MIAGE" }],
	certifications: [{ nom: "Data Engineer" }],
	langues: [{ langue: "Anglais", niveau: "B2" }],
	disponibilite: { immediate: true },
});

// Chaque section vide (cercle vide dans la barre d'étapes) doit avoir au moins un conseil vers son étape.
function verifierCoherence(contenu) {
	const etapesConseillees = new Set(evaluerCv(contenu).aCompleter.map(conseil => conseil.etape));
	for (let etape = 0; etape < SECTIONS; etape += 1) {
		if (!sectionRemplie(contenu, etape)) {
			assert.ok(etapesConseillees.has(etape), `section « ${ETAPES[etape]} » vide sans conseil`);
		}
	}
}

test("un CV complet n'a aucun conseil et toutes ses sections sont remplies", () => {
	const contenu = complet();
	assert.deepEqual(evaluerCv(contenu).aCompleter, []);
	for (let etape = 0; etape < SECTIONS; etape += 1) assert.ok(sectionRemplie(contenu, etape), ETAPES[etape]);
});

test("chaque section vidée donne un conseil vers son étape", () => {
	const vider = {
		competences: c => { c.competences = []; },
		experiences: c => { c.experiences = []; },
		projets: c => { c.projets = []; },
		formations: c => { c.formations = []; },
		certifications: c => { c.certifications = []; },
		langues: c => { c.langues = []; },
	};
	for (const [section, retirer] of Object.entries(vider)) {
		const contenu = complet();
		retirer(contenu);
		verifierCoherence(contenu);
		assert.ok(evaluerCv(contenu).aCompleter.length > 0, section);
	}
});

test("un CV vide a un conseil pour chaque section", () => {
	verifierCoherence({});
	const etapes = new Set(evaluerCv({}).aCompleter.map(conseil => conseil.etape));
	assert.deepEqual([...etapes].sort(), [0, 1, 2, 3, 4, 5, 6]);
});

test("sans expérience, on ne demande pas aussi de la détailler", () => {
	const contenu = complet();
	contenu.experiences = [];
	const conseils = evaluerCv(contenu).aCompleter.filter(conseil => conseil.etape === 2);
	assert.equal(conseils.length, 1);
});
