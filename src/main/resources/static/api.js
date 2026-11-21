const methodesAvecCsrf = new Set(["POST", "PUT", "PATCH", "DELETE"]);

async function obtenirJetonCsrf() {
	const reponse = await fetch("/api/v1/auth/csrf", { cache: "no-store" });
	if (!reponse.ok) {
		throw new Error("Le jeton CSRF est indisponible.");
	}

	const prefixe = "XSRF-TOKEN=";
	const cookie = document.cookie.split(";").map(valeur => valeur.trim())
		.find(valeur => valeur.startsWith(prefixe));
	const jeton = cookie?.slice(prefixe.length);
	if (!jeton) {
		throw new Error("Le cookie CSRF est absent.");
	}
	return jeton;
}

export async function appelerApi(methode, chemin, corps, { redirigerSur401 = true } = {}) {
	const origine = window.location.origin;
	const url = new URL(chemin, origine);
	if (url.origin !== origine) {
		throw new TypeError("appelerApi accepte uniquement les routes de l’application.");
	}

	const method = methode.toUpperCase();
	const headers = new Headers();
	let body = corps;
	if (corps !== undefined && !(corps instanceof URLSearchParams)) {
		headers.set("Content-Type", "application/json");
		body = JSON.stringify(corps);
	}
	if (methodesAvecCsrf.has(method)) {
		headers.set("X-XSRF-TOKEN", await obtenirJetonCsrf());
	}

	const reponse = await fetch(url, { method, headers, body, cache: "no-store" });
	if (redirigerSur401 && reponse.status === 401) {
		window.location.replace("/connexion.html");
	}
	return reponse;
}
