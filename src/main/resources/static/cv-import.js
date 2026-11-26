// Import d'un CV existant (PDF ou DOCX) : boutons [data-importer] et glisser-déposer sur toute la page.
// Partagé par « Mes CV » et « Créer un CV ». Le remplissage automatique par l'IA viendra dans une prochaine étape.

const TAILLE_MAX = 5 * 1024 * 1024;

function verifier(fichier) {
	if (!/\.(pdf|docx)$/i.test(fichier.name)) return "Choisissez un fichier PDF ou DOCX.";
	if (fichier.size > TAILLE_MAX) return "Le fichier dépasse 5 Mo.";
	return `« ${fichier.name} » est prêt. Le remplissage automatique du CV par l'IA arrive dans une prochaine étape.`;
}

// champ : <input type="file" hidden> ; depot : calque affiché pendant le glisser-déposer.
export function brancherImport({ champ, depot, notifier }) {
	const importer = fichier => { if (fichier) notifier(verifier(fichier)); };

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
