import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { test } from "node:test";

import { appelerApi } from "../../main/resources/static/api.js";

function simulerNavigateur(t, reponse) {
	const origine = "https://suivi.example.test";
	const requetes = [];
	const redirections = [];
	globalThis.document = { cookie: "" };
	globalThis.window = { location: { origin: origine, replace: page => redirections.push(page) } };
	t.after(() => {
		delete globalThis.document;
		delete globalThis.window;
	});

	t.mock.method(globalThis, "fetch", async (url, options) => {
		const requete = new Request(new URL(url, origine), options);
		requetes.push(requete);
		if (new URL(requete.url).pathname === "/api/v1/auth/csrf") {
			document.cookie = "XSRF-TOKEN=jeton-de-test";
			return new Response(null, { status: 204 });
		}
		return reponse;
	});
	return { requetes, redirections };
}

test("envoie le JSON et le jeton CSRF sur les quatre méthodes d’écriture", async t => {
	const { requetes } = simulerNavigateur(t, new Response(null, { status: 200 }));
	const corps = { nom: "Alex" };
	for (const methode of ["POST", "PUT", "PATCH", "DELETE"]) {
		await appelerApi(methode, "/api/v1/candidatures", corps);
		const [demandeJeton, ecriture] = requetes.splice(0);
		assert.equal(new URL(demandeJeton.url).pathname, "/api/v1/auth/csrf");
		assert.equal(ecriture.method, methode);
		assert.equal(ecriture.headers.get("X-XSRF-TOKEN"), "jeton-de-test");
		assert.equal(ecriture.headers.get("Content-Type"), "application/json");
		assert.deepEqual(await ecriture.json(), corps);
	}
});

test("envoie le formulaire encodé et laisse le formulaire traiter son 401", async t => {
	const refus = new Response("Identifiants incorrects", { status: 401 });
	const { requetes, redirections } = simulerNavigateur(t, refus);
	const formulaire = new URLSearchParams({ email: "alex@example.test", motDePasse: randomUUID() });
	const reponse = await appelerApi("POST", "/api/v1/auth/connexion", formulaire, { redirigerSur401: false });
	assert.match(requetes[1].headers.get("Content-Type"), /^application\/x-www-form-urlencoded/);
	assert.equal(await requetes[1].text(), formulaire.toString());
	assert.equal(reponse, refus);
	assert.deepEqual(redirections, []);
});

test("envoie un fichier en multipart avec le jeton CSRF", async t => {
	const { requetes } = simulerNavigateur(t, new Response(null, { status: 200 }));
	const donnees = new FormData();
	donnees.append("fichier", new Blob(["Alex Moreau"], { type: "text/plain" }), "cv.txt");
	await appelerApi("POST", "/api/v1/cv/import", donnees);
	const envoi = requetes[1];
	assert.equal(envoi.headers.get("X-XSRF-TOKEN"), "jeton-de-test");
	assert.match(envoi.headers.get("Content-Type"), /^multipart\/form-data; boundary=/);
	assert.equal((await envoi.formData()).get("fichier").name, "cv.txt");
});

test("redirige vers la connexion pour un 401 sur le profil", async t => {
	const { requetes, redirections } = simulerNavigateur(t, new Response(null, { status: 401 }));
	const reponse = await appelerApi("GET", "/api/v1/auth/moi");
	assert.equal(requetes.length, 1);
	assert.equal(requetes[0].headers.has("X-XSRF-TOKEN"), false);
	assert.equal(reponse.status, 401);
	assert.deepEqual(redirections, ["/connexion.html"]);
});

test("accepte un POST sans corps et retourne intacte une réponse 204", async t => {
	const succes = new Response(null, { status: 204 });
	const { requetes } = simulerNavigateur(t, succes);
	assert.equal(await appelerApi("POST", "/api/v1/auth/deconnexion"), succes);
	assert.equal(requetes[1].body, null);
	assert.equal(requetes[1].headers.has("Content-Type"), false);
});

test("refuse une adresse d’une autre origine avant de demander le jeton", async t => {
	const { requetes } = simulerNavigateur(t, new Response(null, { status: 204 }));
	await assert.rejects(appelerApi("POST", "https://autre.example.test/api", {}), TypeError);
	assert.deepEqual(requetes, []);
});
