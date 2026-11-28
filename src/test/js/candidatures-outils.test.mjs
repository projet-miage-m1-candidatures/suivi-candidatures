import assert from "node:assert/strict";
import { test } from "node:test";

import {
	analyserGras, analyserMarkdown, formaterDate, lienSur, messageErreur,
} from "../../main/resources/static/candidatures-outils.js";

test("découpe la fiche de l'IA en titres, listes et paragraphes", () => {
	const blocs = analyserMarkdown("## L'entreprise en bref\nUne PME.\nÀ vérifier.\n\n- point 1\n- point 2\n1. question\n### Sous-titre");
	assert.deepEqual(blocs, [
		{ type: "titre", niveau: 2, texte: "L'entreprise en bref" },
		{ type: "paragraphe", texte: "Une PME. À vérifier." },
		{ type: "liste", ordonnee: false, elements: ["point 1", "point 2"] },
		{ type: "liste", ordonnee: true, elements: ["question"] },
		{ type: "titre", niveau: 3, texte: "Sous-titre" },
	]);
});

test("garde le HTML de l'IA comme du texte", () => {
	assert.deepEqual(analyserMarkdown("<img src=x onerror=alert(1)>"),
		[{ type: "paragraphe", texte: "<img src=x onerror=alert(1)>" }]);
});

test("repère le gras", () => {
	assert.deepEqual(analyserGras("Pourquoi : **motivation** ici"), [
		{ texte: "Pourquoi : ", gras: false },
		{ texte: "motivation", gras: true },
		{ texte: " ici", gras: false },
	]);
});

test("refuse les liens qui ne sont pas http(s)", () => {
	assert.equal(lienSur("javascript:alert(1)"), null);
	assert.equal(lienSur("pas une url"), null);
	assert.equal(lienSur("https://exemple.fr/offre"), "https://exemple.fr/offre");
});

test("formate une date sans décalage de fuseau", () => {
	assert.match(formaterDate("2026-11-26", "long"), /26 nov\.? 2026/);
	assert.equal(formaterDate(null), "");
});

test("affiche le détail renvoyé par Spring, sinon un message par statut", async () => {
	const avecDetail = new Response(JSON.stringify({ detail: "Transition interdite" }), { status: 409 });
	assert.equal(await messageErreur(avecDetail), "Transition interdite");
	assert.equal(await messageErreur(new Response(null, { status: 503 })), "L'assistant IA est indisponible pour le moment.");
});
