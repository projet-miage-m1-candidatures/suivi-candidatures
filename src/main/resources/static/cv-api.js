// Accès aux CV.
// VERSION DE DÉMONSTRATION : les CV sont des données fictives gardées dans le navigateur.
// À l'étape « branchement », seul l'intérieur de ces fonctions changera pour appeler l'API
// (/api/v1/cv) avec appelerApi ; les pages, elles, ne changeront pas.

const CLE = "suivi-candidatures.cv-demo.v1";
let memoire = null;

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
	return copie(tous()).sort((a, b) => b.dateModification.localeCompare(a.dateModification));
}

export async function lireCv(id) {
	const cv = tous().find(c => c.id === Number(id));
	if (!cv) throw new ErreurCv(404, "Ce CV n'existe pas.");
	return copie(cv);
}

export async function creerCv(cv) {
	valider(cv);
	const liste = tous();
	const maintenant = new Date().toISOString();
	const nouveau = { ...copie(cv), id: Math.max(0, ...liste.map(c => c.id)) + 1, dateCreation: maintenant, dateModification: maintenant };
	ecrire([...liste, nouveau]);
	return copie(nouveau);
}

export async function enregistrerCv(cv) {
	valider(cv);
	const liste = tous();
	const index = liste.findIndex(c => c.id === cv.id);
	if (index < 0) throw new ErreurCv(404, "Ce CV n'existe pas.");
	const misAJour = { ...copie(cv), dateModification: new Date().toISOString() };
	liste[index] = misAJour;
	ecrire(liste);
	return copie(misAJour);
}

export async function supprimerCv(id) {
	ecrire(tous().filter(c => c.id !== Number(id)));
}

export async function dupliquerCv(id) {
	const { id: _id, dateCreation, dateModification, ...cv } = await lireCv(id);
	return creerCv({ ...cv, titre: `${cv.titre} (copie)` });
}

export async function reinitialiserDemo() {
	ecrire(exemples());
}

function valider(cv) {
	if (!cv.titre?.trim()) throw new ErreurCv(400, "Le titre du CV est obligatoire.");
	if (!cv.contenu?.identite?.nom?.trim()) throw new ErreurCv(400, "Le nom est obligatoire.");
}

function copie(objet) {
	return structuredClone(objet);
}

function tous() {
	if (!memoire) {
		try {
			memoire = JSON.parse(localStorage.getItem(CLE) ?? "null");
		} catch {
			memoire = null;
		}
		memoire ??= exemples();
	}
	return memoire;
}

function ecrire(liste) {
	memoire = liste;
	try {
		localStorage.setItem(CLE, JSON.stringify(liste));
	} catch {
		// Stockage indisponible : les données restent en mémoire jusqu'au rechargement.
	}
}

function exemples() {
	const identite = {
		nom: "Alex Moreau",
		titre: "Data engineer junior · recherche d'alternance",
		ville: "Paris",
		email: "alex.moreau@exemple.fr",
		telephone: "06 12 34 56 78",
		lien: "github.com/alex-moreau",
		profil: "Étudiant en M1 MIAGE, je conçois des pipelines de données fiables avec SQL, Python et Airflow. Je cherche une alternance pour industrialiser des flux de données au sein d'une équipe data.",
	};
	const stage = {
		poste: "Data engineer (stage)", entreprise: "Nordlys", lieu: "Paris", debut: "04/2026", fin: "08/2026",
		realisations: [
			"Conçu un pipeline ELT vers PostgreSQL orchestré avec Airflow",
			"Mis en place des tests de qualité des données avec dbt",
			"Réduit de 40 % la durée du chargement quotidien",
		],
	};
	const job = {
		poste: "Analyste de données (job étudiant)", entreprise: "Nova Retail", lieu: "Lyon", debut: "2024", fin: "2025",
		realisations: [
			"Automatisé les rapports hebdomadaires en Python",
			"Construit un tableau de bord des ventes pour 12 magasins",
		],
	};
	const formations = [
		{ diplome: "Master MIAGE (M1)", ecole: "Université Paris Nanterre", debut: "2026", fin: "2027" },
		{ diplome: "Licence informatique", ecole: "Université Lyon 1", debut: "2023", fin: "2026" },
	];
	const competences = [
		{ groupe: "Langages", elements: ["SQL", "Python", "Java"] },
		{ groupe: "Données", elements: ["PostgreSQL", "Airflow", "dbt", "Spark"] },
		{ groupe: "Outils", elements: ["Git", "Docker", "Linux"] },
	];
	const langues = [
		{ langue: "Français", niveau: "Langue maternelle" },
		{ langue: "Anglais", niveau: "C1" },
		{ langue: "Espagnol", niveau: "B1" },
	];
	return [
		{
			id: 1, titre: "Data engineer", langue: "FR", modele: "CLASSIQUE", police: "SOURCE_SERIF_4",
			dateCreation: "2026-11-10T09:00:00Z", dateModification: "2026-11-25T17:20:00Z",
			contenu: { identite, experiences: [stage, job], formations, competences, langues },
		},
		{
			id: 2, titre: "Data engineer (anglais)", langue: "EN", modele: "MODERNE", police: "INTER",
			dateCreation: "2026-11-12T09:00:00Z", dateModification: "2026-11-24T10:05:00Z",
			contenu: {
				identite: { ...identite, titre: "Junior data engineer · seeking a work-study position", profil: "First-year master's student in information systems (MIAGE). I build reliable data pipelines with SQL, Python and Airflow." },
				experiences: [
					{ ...stage, poste: "Data engineer (internship)", realisations: ["Built an ELT pipeline into PostgreSQL orchestrated with Airflow", "Added data quality tests with dbt", "Cut the daily load time by 40%"] },
					{ ...job, poste: "Data analyst (student job)", realisations: ["Automated weekly reports in Python", "Built a sales dashboard for 12 stores"] },
				],
				formations: [{ diplome: "Master's in information systems (M1)", ecole: "Paris Nanterre University", debut: "2026", fin: "2027" }],
				competences,
				langues: [{ langue: "French", niveau: "Native" }, { langue: "English", niveau: "C1" }],
			},
		},
		{
			id: 3, titre: "Data analyst", langue: "FR", modele: "COMPACT", police: "IBM_PLEX_SANS",
			dateCreation: "2026-11-05T09:00:00Z", dateModification: "2026-11-18T14:40:00Z",
			contenu: {
				identite: { ...identite, titre: "Data analyst", profil: "" },
				experiences: [{ ...job, realisations: ["Automatisé les rapports hebdomadaires en Python"] }],
				formations,
				competences: [{ groupe: "Analyse", elements: ["SQL", "Python", "Power BI"] }],
				langues: [],
			},
		},
		{
			id: 4, titre: "Développeur Java", langue: "FR", modele: "MODERNE", police: "LATO",
			dateCreation: "2026-11-26T08:00:00Z", dateModification: "2026-11-26T08:15:00Z",
			contenu: {
				identite: { ...identite, titre: "Développeur Java junior", profil: "" },
				experiences: [], formations: [], competences: [], langues: [],
			},
		},
	];
}
