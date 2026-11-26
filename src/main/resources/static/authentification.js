import { appelerApi } from "./api.js";

const apiAuth = "/api/v1/auth";
const messageIndisponible = "La demande a échoué. Réessayez dans un instant.";

function afficherErreur(zone, message) {
	zone.textContent = message;
	zone.hidden = !message;
}

async function ouvrirApresRenouvellement(page) {
	// Le prochain POST demandera lui aussi un jeton si le réseau est indisponible ici.
	await appelerApi("GET", `${apiAuth}/csrf`, undefined, { redirigerSur401: false }).catch(() => {});
	window.location.replace(page);
}

const formulaire = document.querySelector("[data-formulaire-auth]");
if (formulaire) {
	const bouton = formulaire.querySelector("button[type=submit]");
	const erreur = formulaire.querySelector("[data-erreur-auth]");
	const route = formulaire.dataset.formulaireAuth;
	const connexion = route === "connexion";

	formulaire.addEventListener("submit", async evenement => {
		evenement.preventDefault();
		if (bouton.disabled) return;

		bouton.disabled = true;
		afficherErreur(erreur, "");
		const champs = new FormData(formulaire);
		const corps = connexion ? new URLSearchParams(champs) : Object.fromEntries(champs);

		try {
			const reponse = await appelerApi("POST", `${apiAuth}/${route}`, corps, { redirigerSur401: false });
			if (connexion && reponse.status === 200) {
				await ouvrirApresRenouvellement("/tableau-de-bord.html");
			} else if (!connexion && reponse.status === 201) {
				window.location.replace("/connexion.html");
			} else {
				const messages = {
					400: "Vérifiez vos informations. Le mot de passe doit contenir au moins 15 caractères et au plus 72 octets.",
					401: "Adresse e-mail ou mot de passe incorrect.",
					403: "La vérification de sécurité a échoué. Réessayez.",
					409: "Cette adresse e-mail est déjà utilisée.",
				};
				afficherErreur(erreur, messages[reponse.status] ?? messageIndisponible);
			}
		} catch {
			afficherErreur(erreur, messageIndisponible);
		} finally {
			bouton.disabled = false;
		}
	});
}

const compte = document.querySelector("[data-compte-candidat]");
if (compte) {
	const erreur = compte.querySelector("[data-erreur-auth]");
	const deconnexion = compte.querySelector("[data-deconnexion]");

	async function chargerProfil() {
		try {
			const reponse = await appelerApi("GET", `${apiAuth}/moi`);
			if (reponse.status === 401) return;
			if (!reponse.ok) throw new Error("Le profil est indisponible.");

			const { prenom, nom } = await reponse.json();
			compte.querySelector("[data-nom-candidat]").textContent = `${prenom} ${nom}`;
			compte.querySelector("[data-initiales-candidat]").textContent =
				`${Array.from(prenom.trim())[0] ?? ""}${Array.from(nom.trim())[0] ?? ""}`.toUpperCase();
			const salutation = document.querySelector("[data-salutation-candidat]");
			if (salutation) salutation.textContent = `Bonjour, ${prenom}`;
		} catch {
			afficherErreur(erreur, "Impossible de charger le profil. Actualisez la page.");
		}
	}

	deconnexion.addEventListener("click", async () => {
		deconnexion.disabled = true;
		afficherErreur(erreur, "");
		try {
			const reponse = await appelerApi("POST", `${apiAuth}/deconnexion`);
			if (reponse.status === 401) return;
			if (!reponse.ok) throw new Error("La déconnexion a échoué.");
			await ouvrirApresRenouvellement("/connexion.html");
		} catch {
			afficherErreur(erreur, "Impossible de vous déconnecter. Réessayez.");
		} finally {
			deconnexion.disabled = false;
		}
	});

	chargerProfil();
	window.addEventListener("pageshow", evenement => {
		if (evenement.persisted) chargerProfil();
	});
}
