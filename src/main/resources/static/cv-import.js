// Import d'un CV existant, dans une fenêtre dédiée : choisir le fichier, le voir, puis lancer l'analyse par l'IA.
// Partagé par « Mes CV » et « Créer un CV ». L'import crée toujours un NOUVEAU CV, enregistré comme brouillon :
// un CV existant n'est jamais remplacé.
import { nouveauCv, creerCv, importerCv } from "./cv-api.js";

const TAILLE_MAX = 10 * 1024 * 1024;
const FORMATS = /\.(pdf|docx|txt|png|jpe?g)$/i;
// CV importé sans prénom ou nom : il ne peut pas encore être enregistré, on le confie à la page suivante.
const CLE_A_COMPLETER = "cv-importe-a-completer";

const ICONE_FICHIER = `<svg aria-hidden="true" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/></svg>`;
const ICONE_ENVOI = `<svg aria-hidden="true" viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 15V4M7.5 8.5 12 4l4.5 4.5M5 15v3a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-3"/></svg>`;
const ICONE_COCHE = `<svg aria-hidden="true" viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M3.5 8.5l3 3 6-7"/></svg>`;

const ETAPES_ANALYSE = ["Lecture du fichier", "Analyse par l'intelligence artificielle", "Création du brouillon"];

function erreurFichier(fichier) {
	if (!FORMATS.test(fichier.name)) return "Ce format n'est pas accepté. Choisissez un PDF, un Word (DOCX), un fichier texte (TXT) ou une image (PNG, JPG).";
	if (fichier.size > TAILLE_MAX) return "Le fichier dépasse 10 Mo.";
	if (fichier.size === 0) return "Le fichier est vide.";
	return "";
}

function taille(octets) {
	return octets < 1024 * 1024 ? `${Math.max(1, Math.round(octets / 1024))} Ko` : `${(octets / 1024 / 1024).toFixed(1).replace(".", ",")} Mo`;
}

function echapper(texte) {
	return String(texte).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}

function titreImporte(contenu, fichier) {
	const titre = contenu.identite?.titre?.trim() || fichier.name.replace(/\.[^.]+$/, "").trim() || "CV importé";
	return titre.slice(0, 120);
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

// Première page d'un PDF, dessinée par PDF.js (Mozilla, licence Apache 2.0) : un aperçu net, sans le cadre sombre
// du lecteur PDF du navigateur. La bibliothèque n'est chargée qu'au premier PDF choisi.
async function dessinerPremierePage(fichier, toile, largeur) {
	const pdfjs = await import("./vendor/pdfjs/pdf.min.js");
	pdfjs.GlobalWorkerOptions.workerSrc = new URL("./vendor/pdfjs/pdf.worker.min.js", import.meta.url).href;
	const pdf = await pdfjs.getDocument({ data: await fichier.arrayBuffer() }).promise;
	try {
		const page = await pdf.getPage(1);
		const echelle = largeur / page.getViewport({ scale: 1 }).width * (window.devicePixelRatio || 1);
		const vue = page.getViewport({ scale: echelle });
		toile.width = Math.floor(vue.width);
		toile.height = Math.floor(vue.height);
		await page.render({ canvasContext: toile.getContext("2d"), viewport: vue }).promise;
	} finally {
		await pdf.destroy();
	}
}

function creerFenetre() {
	const fenetre = document.createElement("dialog");
	fenetre.className = "fenetre-import";
	fenetre.setAttribute("aria-labelledby", "titre-import");
	fenetre.innerHTML = `
<button type="button" class="fermer-import" data-fermer-import aria-label="Fermer">×</button>
<h2 id="titre-import">Importer votre CV</h2>
<p class="intro-import">Ajoutez votre CV actuel : l'intelligence artificielle le lit et remplit le formulaire pour vous. Vous relisez tout avant de le télécharger.</p>
<div data-vue="choix">
	<label class="zone-import" data-zone-import>
		<input type="file" accept=".pdf,.docx,.txt,.png,.jpg,.jpeg" data-champ-import hidden>
		<span class="icone-import">${ICONE_ENVOI}</span>
		<strong>Glissez votre fichier ici</strong>
		<span>ou <span class="lien-import">choisissez-le sur votre ordinateur</span></span>
		<small>PDF, Word (DOCX), texte (TXT) ou image (PNG, JPG) · 10 Mo maximum</small>
	</label>
	<div class="fichier-import" data-fichier-import hidden></div>
	<p class="erreur-import" data-erreur-import role="alert" hidden></p>
	<p class="note-import">Votre CV sera analysé par une intelligence artificielle (OpenAI). Le fichier n'est pas conservé.</p>
	<div class="actions-import">
		<button type="button" class="bouton-import" data-fermer-import>Annuler</button>
		<button type="button" class="bouton-import principal" data-lancer-import disabled>Importer</button>
	</div>
</div>
<div data-vue="analyse" hidden>
	<p class="message-analyse">Patientez quelques secondes : votre CV est en cours d'analyse par une intelligence artificielle.</p>
	<div class="progression-import" role="progressbar" aria-label="Analyse du CV" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">
		<span data-barre-import></span>
	</div>
	<p class="pourcentage-import" data-pourcentage-import>0 %</p>
	<ol class="etapes-import" data-etapes-import></ol>
	<p class="note-import">Étapes indicatives. Gardez cette page ouverte.</p>
</div>`;
	document.body.append(fenetre);
	return fenetre;
}

// depot : calque affiché pendant un glisser-déposer sur la page.
// avantImport : appelé avant l'analyse, renvoie false pour l'annuler (ex. le CV ouvert n'a pas pu être enregistré).
export function brancherImport({ depot, avantImport = async () => true }) {
	const fenetre = creerFenetre();
	const $ = selecteur => fenetre.querySelector(selecteur);
	const etat = { fichier: null, apercu: null, minuteur: null, enCours: false };

	const vue = nom => {
		fenetre.querySelectorAll("[data-vue]").forEach(bloc => { bloc.hidden = bloc.dataset.vue !== nom; });
		$(".fermer-import").hidden = nom === "analyse"; // l'analyse ne peut pas être interrompue
	};

	const afficherErreur = message => {
		$("[data-erreur-import]").textContent = message;
		$("[data-erreur-import]").hidden = !message;
	};

	// Aperçu du fichier choisi : l'image, la première page du PDF, ou le début du texte pour un TXT.
	const choisirFichier = async fichier => {
		if (!fichier) return;
		if (etat.apercu) URL.revokeObjectURL(etat.apercu);
		etat.apercu = null;
		const erreur = erreurFichier(fichier);
		afficherErreur(erreur);
		etat.fichier = erreur ? null : fichier;
		$("[data-lancer-import]").disabled = Boolean(erreur);
		const bloc = $("[data-fichier-import]");
		bloc.hidden = Boolean(erreur);
		$("[data-zone-import]").hidden = !erreur;
		if (erreur) return;
		let apercu = "";
		if (/^image\//.test(fichier.type)) {
			etat.apercu = URL.createObjectURL(fichier);
			apercu = `<img src="${etat.apercu}" alt="Aperçu de ${echapper(fichier.name)}">`;
		} else if (/\.pdf$/i.test(fichier.name)) {
			apercu = `<div class="page-pdf"><canvas data-page-pdf role="img" aria-label="Première page de ${echapper(fichier.name)}"></canvas></div>`;
		} else if (/\.txt$/i.test(fichier.name)) {
			apercu = `<pre>${echapper((await fichier.slice(0, 2000).text()).trim())}</pre>`;
		}
		bloc.innerHTML = `<div class="entete-fichier"><span class="icone-fichier">${ICONE_FICHIER}</span>
<span><strong>${echapper(fichier.name)}</strong><small>${taille(fichier.size)}</small></span>
<button type="button" class="lien-import" data-changer-import>Changer de fichier</button></div>
${apercu ? `<div class="apercu-fichier">${apercu}</div>` : ""}`;
		const toile = bloc.querySelector("[data-page-pdf]");
		if (toile) {
			// PDF illisible ou bibliothèque indisponible : on garde seulement le nom et la taille du fichier.
			dessinerPremierePage(fichier, toile, Math.max(toile.clientWidth, 320))
				.catch(() => { if (etat.fichier === fichier) toile.closest(".apercu-fichier")?.remove(); });
		}
	};

	// La durée réelle est inconnue : la barre avance vite puis ralentit, et n'atteint 100 % qu'à la réponse.
	const lancerProgression = () => {
		const debut = performance.now();
		const afficher = pourcent => {
			$("[data-barre-import]").style.width = `${pourcent}%`;
			$("[data-pourcentage-import]").textContent = `${pourcent} %`;
			fenetre.querySelector("[role=progressbar]").setAttribute("aria-valuenow", String(pourcent));
			const etape = pourcent >= 100 ? 3 : pourcent >= 12 ? 1 : 0;
			$("[data-etapes-import]").innerHTML = ETAPES_ANALYSE.map((texte, i) =>
				`<li class="${i < etape ? "faite" : i === etape ? "en-cours" : ""}"><span class="marque-import">${i < etape ? ICONE_COCHE : ""}</span>${texte}</li>`).join("");
		};
		afficher(0);
		etat.minuteur = setInterval(() => {
			const secondes = (performance.now() - debut) / 1000;
			afficher(Math.min(95, Math.round(95 * (1 - Math.exp(-secondes / 9)))));
		}, 200);
		return afficher;
	};

	const ouvrir = fichier => {
		if (etat.enCours) return;
		vue("choix");
		etat.fichier = null;
		$("[data-zone-import]").hidden = false;
		$("[data-fichier-import]").hidden = true;
		$("[data-lancer-import]").disabled = true;
		afficherErreur("");
		if (!fenetre.open) fenetre.showModal();
		if (fichier) choisirFichier(fichier);
	};

	const fermer = () => {
		if (etat.enCours) return;
		if (etat.apercu) URL.revokeObjectURL(etat.apercu);
		etat.apercu = null;
		fenetre.close();
	};

	const lancer = async () => {
		const fichier = etat.fichier;
		if (!fichier || etat.enCours) return;
		if (!(await avantImport())) {
			fermer();
			return;
		}
		etat.enCours = true;
		vue("analyse");
		const afficher = lancerProgression();
		try {
			const resultat = await importerCv(fichier);
			const cv = { ...nouveauCv(), titre: titreImporte(resultat.contenu, fichier), langue: resultat.langue === "EN" ? "EN" : "FR", contenu: resultat.contenu };
			try {
				const cree = await creerCv(cv);
				clearInterval(etat.minuteur);
				afficher(100);
				window.location.assign(`creer-cv.html?id=${encodeURIComponent(cree.id)}&etape=verification&importe=1`);
			} catch (refus) {
				if (refus.statut !== 400) throw refus;
				// Prénom ou nom introuvable dans le fichier : le candidat les ajoute, puis le CV s'enregistre.
				sessionStorage.setItem(CLE_A_COMPLETER, JSON.stringify(cv));
				clearInterval(etat.minuteur);
				afficher(100);
				window.location.assign("creer-cv.html?importe=a-completer");
			}
		} catch (echec) {
			clearInterval(etat.minuteur);
			etat.enCours = false;
			vue("choix");
			afficherErreur(echec.message || "L'import a échoué. Réessayez.");
		}
	};

	// Fenêtre
	fenetre.addEventListener("click", evenement => {
		if (evenement.target === fenetre || evenement.target.closest("[data-fermer-import]")) fermer();
		else if (evenement.target.closest("[data-changer-import]")) $("[data-champ-import]").click();
		else if (evenement.target.closest("[data-lancer-import]")) lancer();
	});
	fenetre.addEventListener("cancel", evenement => {
		if (etat.enCours) evenement.preventDefault();
		else fermer();
	});
	$("[data-champ-import]").addEventListener("change", evenement => {
		choisirFichier(evenement.target.files[0]);
		evenement.target.value = "";
	});

	// Boutons « Importer » de la page
	document.addEventListener("click", evenement => {
		if (evenement.target.closest("[data-importer]")) ouvrir();
	});

	// Glisser-déposer : sur la zone de la fenêtre, ou n'importe où sur la page (la fenêtre s'ouvre alors).
	let survols = 0;
	const contientFichier = evenement => [...(evenement.dataTransfer?.types ?? [])].includes("Files");
	document.addEventListener("dragenter", evenement => {
		if (!contientFichier(evenement)) return;
		survols += 1;
		if (fenetre.open) $("[data-zone-import]").classList.add("survol");
		else depot.hidden = false;
	});
	document.addEventListener("dragleave", evenement => {
		if (!contientFichier(evenement)) return;
		survols = Math.max(0, survols - 1);
		if (survols === 0) {
			depot.hidden = true;
			$("[data-zone-import]").classList.remove("survol");
		}
	});
	document.addEventListener("dragover", evenement => {
		if (contientFichier(evenement)) evenement.preventDefault();
	});
	document.addEventListener("drop", evenement => {
		if (!contientFichier(evenement)) return;
		evenement.preventDefault();
		survols = 0;
		depot.hidden = true;
		$("[data-zone-import]").classList.remove("survol");
		if (etat.enCours) return;
		if (fenetre.open) choisirFichier(evenement.dataTransfer.files[0]);
		else ouvrir(evenement.dataTransfer.files[0]);
	});
}
