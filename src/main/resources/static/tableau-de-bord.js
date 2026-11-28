// Tableau de bord : chiffres et dernières candidatures du candidat connecté.
import { appelerApi } from "./api.js";
import { apiCandidatures, creerBadge, formaterDate, lireDate, messageErreur, statuts } from "./candidatures-outils.js";

const $ = selecteur => document.querySelector(selecteur);

function ligneCandidature(candidature, avecBordure = true) {
	const item = document.createElement("li");
	const lien = document.createElement("a");
	lien.href = `mes-candidatures.html#candidature-${candidature.id}`;
	lien.style.cssText = "display:flex;flex-wrap:wrap;align-items:center;gap:8px 16px;padding:14px 0;color:#0f2a5c"
		+ (avecBordure ? ";border-top:1px solid #e3edfc" : "");
	const texte = document.createElement("div");
	texte.style.cssText = "flex:1 1 220px;min-width:0";
	const titre = document.createElement("div");
	titre.style.cssText = "font-size:15.5px;font-weight:700;overflow-wrap:anywhere";
	titre.textContent = candidature.intitule;
	const sousTitre = document.createElement("div");
	sousTitre.style.cssText = "font-size:13.5px;color:#4a5f86;overflow-wrap:anywhere";
	sousTitre.textContent = [candidature.entreprise, candidature.lieu].filter(Boolean).join(" · ");
	texte.append(titre, sousTitre);
	const date = document.createElement("span");
	date.style.cssText = "width:84px;font-size:13.5px;color:#4a5f86";
	date.textContent = formaterDate(candidature.dateCandidature, "long");
	lien.append(texte, creerBadge(candidature.statut), date);
	item.append(lien);
	return item;
}

function afficherCompteurs(compteurs) {
	const total = Object.values(compteurs).reduce((somme, n) => somme + n, 0);
	$("[data-total-candidatures]").textContent = total;
	$("[data-total-libelle]").textContent = total > 1 ? "candidatures suivies" : "candidature suivie";

	const barre = $("[data-barre-statuts]");
	const liste = $("[data-compteurs-statuts]");
	barre.replaceChildren();
	liste.replaceChildren();
	const resume = [];
	for (const [code, style] of Object.entries(statuts)) {
		const nombre = compteurs[code] ?? 0;
		resume.push(`${nombre} ${style.libelle.toLowerCase()}`);
		if (total && nombre) {
			const part = document.createElement("div");
			part.style.cssText = `width:${(100 * nombre / total).toFixed(1)}%;background:${style.point}`;
			barre.append(part);
		}
		const item = document.createElement("li");
		item.style.cssText = "flex:1 1 120px;box-sizing:border-box;padding:12px 14px;border-radius:16px;background:#f1f6fd";
		const libelle = document.createElement("div");
		libelle.style.cssText = "display:flex;align-items:center;gap:8px;font-size:13.5px;color:#35476b";
		const point = document.createElement("span");
		point.setAttribute("aria-hidden", "true");
		point.style.cssText = `width:9px;height:9px;border-radius:50%;background:${style.point}`;
		libelle.append(point, style.libelle);
		const chiffre = document.createElement("div");
		chiffre.style.cssText = "font-family:'Instrument Serif',Georgia,serif;font-size:36px;line-height:1.1";
		chiffre.textContent = nombre;
		item.append(libelle, chiffre);
		liste.append(item);
	}
	barre.setAttribute("aria-label", total ? `Répartition : ${resume.join(", ")}` : "Aucune candidature");
}

function afficherProchainEntretien(candidatures) {
	const aujourdhui = new Date();
	aujourdhui.setHours(0, 0, 0, 0);
	const entretiens = candidatures.filter(c => c.statut === "ENTRETIEN");
	const avecDate = entretiens.filter(c => c.dateEntretien && lireDate(c.dateEntretien) >= aujourdhui)
		.sort((a, b) => lireDate(a.dateEntretien) - lireDate(b.dateEntretien));
	const prochain = avecDate[0] ?? entretiens[0];
	if (!prochain) return;
	const date = $("[data-prochain-date]");
	date.textContent = prochain.dateEntretien ? formaterDate(prochain.dateEntretien, "jour") : "Date à définir";
	date.style.textTransform = "none";
	$("[data-prochain-poste]").textContent = [prochain.intitule, prochain.entreprise, prochain.lieu]
		.filter(Boolean).join(" · ");
	const lien = $("[data-prochain-lien]");
	lien.href = `mes-candidatures.html#candidature-${prochain.id}`;
	lien.hidden = false;
}

function afficherEnAttente(candidatures) {
	const liste = $("[data-en-attente]");
	liste.replaceChildren();
	const enAttente = candidatures.filter(c => c.statut === "EN_ATTENTE").slice(-3).reverse();
	if (!enAttente.length) {
		const vide = document.createElement("li");
		vide.style.cssText = "font-size:14px;color:#4a5f86";
		vide.textContent = "Rien en attente.";
		liste.append(vide);
		return;
	}
	for (const candidature of enAttente) {
		const jours = Math.floor((Date.now() - lireDate(candidature.dateCandidature)) / 86_400_000);
		const item = document.createElement("li");
		const lien = document.createElement("a");
		lien.href = `mes-candidatures.html#candidature-${candidature.id}`;
		lien.style.cssText = "display:block;color:#0f2a5c";
		const titre = document.createElement("div");
		titre.style.cssText = "font-size:15px;font-weight:600;overflow-wrap:anywhere";
		titre.textContent = `${candidature.intitule} · ${candidature.entreprise}`;
		const depuis = document.createElement("div");
		depuis.style.cssText = "font-size:13.5px;color:#4a5f86";
		depuis.textContent = jours <= 0 ? "Envoyée aujourd'hui" : `Envoyée il y a ${jours} jour${jours > 1 ? "s" : ""}`;
		lien.append(titre, depuis);
		item.append(lien);
		liste.append(item);
	}
}

async function charger() {
	const erreur = $("[data-erreur-tableau]");
	try {
		const [reponseListe, reponseCompteurs] = await Promise.all([
			appelerApi("GET", apiCandidatures),
			appelerApi("GET", `${apiCandidatures}/compteurs`),
		]);
		if (reponseListe.status === 401 || reponseCompteurs.status === 401) return;
		if (!reponseListe.ok) throw new Error(await messageErreur(reponseListe));
		if (!reponseCompteurs.ok) throw new Error(await messageErreur(reponseCompteurs));
		const candidatures = await reponseListe.json();
		afficherCompteurs(await reponseCompteurs.json());
		afficherProchainEntretien(candidatures);
		afficherEnAttente(candidatures);
		const dernieres = $("[data-dernieres-candidatures]");
		dernieres.replaceChildren(...candidatures.slice(0, 5).map(c => ligneCandidature(c)));
		$("[data-aucune-candidature]").hidden = candidatures.length > 0;
	} catch (e) {
		erreur.textContent = e.message || "Impossible de charger le tableau de bord.";
		erreur.hidden = false;
	}
}
charger();
