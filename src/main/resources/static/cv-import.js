// Import d'un CV existant : boutons [data-importer] et glisser-déposer sur toute la page.
// Partagé par « Mes CV » et « Créer un CV ». L'import crée toujours un NOUVEAU CV, enregistré comme brouillon :
// un CV existant n'est jamais remplacé.
import { nouveauCv, creerCv, importerCv } from "./cv-api.js";

const TAILLE_MAX = 5 * 1024 * 1024;
const FORMATS = /\.(pdf|docx|txt|png|jpe?g)$/i;
// CV importé sans prénom ou nom : il ne peut pas encore être enregistré, on le confie à la page suivante.
const CLE_A_COMPLETER = "cv-importe-a-completer";

function erreurFichier(fichier) {
	if (!FORMATS.test(fichier.name)) return "Choisissez un fichier PDF, Word (DOCX), texte (TXT) ou une image (PNG, JPG).";
	if (fichier.size > TAILLE_MAX) return "Le fichier dépasse 5 Mo.";
	if (fichier.size === 0) return "Le fichier est vide.";
	return "";
}

function titreImporte(contenu, fichier) {
	const titre = contenu.identite?.titre?.trim() || fichier.name.replace(/\.[^.]+$/, "").trim() || "CV importé";
	return titre.slice(0, 120);
}

// Calque d'attente : l'IA peut mettre de 10 à 30 secondes.
function creerAttente() {
	const attente = document.createElement("div");
	attente.className = "depot";
	attente.hidden = true;
	attente.setAttribute("role", "status");
	attente.innerHTML = `<span style="max-width:30em;text-align:center">Lecture de votre CV…
<small style="display:block;margin-top:10px;font-size:15px;font-weight:500">L'IA range son contenu dans le formulaire, cela peut prendre jusqu'à 30 secondes.
Le contenu est envoyé à OpenAI pour être analysé ; le fichier n'est pas conservé.</small></span>`;
	document.body.append(attente);
	return attente;
}

// Récupère (une seule fois) un CV importé qui attend son prénom ou son nom.
export function reprendreCvImporte() {
	try {
		const texte = sessionStorage.getItem(CLE_A_COMPLETER);
		sessionStorage.removeItem(CLE_A_COMPLETER);
		return texte ? JSON.parse(texte) : null;
	} catch {
		return null;
	}
}

// champ : <input type="file" hidden> ; depot : calque du glisser-déposer ;
// avantImport : appelé avant l'import, renvoie false pour l'annuler (ex. le CV ouvert n'a pas pu être enregistré).
export function brancherImport({ champ, depot, notifier, avantImport = async () => true }) {
	const attente = creerAttente();
	let enCours = false;

	const importer = async fichier => {
		if (!fichier || enCours) return;
		const erreur = erreurFichier(fichier);
		if (erreur) {
			notifier(erreur);
			return;
		}
		if (!(await avantImport())) return;
		enCours = true;
		attente.hidden = false;
		try {
			const resultat = await importerCv(fichier);
			const cv = { ...nouveauCv(), titre: titreImporte(resultat.contenu, fichier), langue: resultat.langue === "EN" ? "EN" : "FR", contenu: resultat.contenu };
			try {
				const cree = await creerCv(cv);
				window.location.assign(`creer-cv.html?id=${encodeURIComponent(cree.id)}&etape=verification&importe=1`);
			} catch (refus) {
				if (refus.statut !== 400) throw refus;
				// Prénom ou nom introuvable dans le fichier : le candidat les ajoute, puis enregistre.
				sessionStorage.setItem(CLE_A_COMPLETER, JSON.stringify(cv));
				window.location.assign("creer-cv.html?importe=a-completer");
			}
		} catch (echec) {
			attente.hidden = true;
			enCours = false;
			notifier(echec.message || "L'import a échoué. Réessayez.");
		}
	};

	document.addEventListener("click", evenement => {
		if (evenement.target.closest("[data-importer]")) champ.click();
	});
	champ.addEventListener("change", () => {
		importer(champ.files[0]);
		champ.value = "";
	});

	let survols = 0;
	const contientFichier = evenement => [...(evenement.dataTransfer?.types ?? [])].includes("Files");
	document.addEventListener("dragenter", evenement => {
		if (!contientFichier(evenement)) return;
		survols += 1;
		depot.hidden = false;
	});
	document.addEventListener("dragleave", evenement => {
		if (!contientFichier(evenement)) return;
		survols = Math.max(0, survols - 1);
		if (survols === 0) depot.hidden = true;
	});
	document.addEventListener("dragover", evenement => {
		if (contientFichier(evenement)) evenement.preventDefault();
	});
	document.addEventListener("drop", evenement => {
		if (!contientFichier(evenement)) return;
		evenement.preventDefault();
		survols = 0;
		depot.hidden = true;
		importer(evenement.dataTransfer.files[0]);
	});
}
