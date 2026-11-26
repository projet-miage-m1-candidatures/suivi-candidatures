package fr.miage.suivicandidatures.cv;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

final class DatesCv {

	static final String ANNEE_MOIS = "(?:[0-9]{4}(?:-(?:0[1-9]|1[0-2]))?)?";
	static final String MESSAGE_DEBUT = "La date de début doit être au format AAAA ou AAAA-MM.";
	static final String MESSAGE_FIN = "La date de fin doit être au format AAAA ou AAAA-MM.";

	private static final Pattern MOIS_ISO = Pattern.compile("([0-9]{4})-([0-9]{2})");
	private static final Pattern MOIS_ANCIEN = Pattern.compile("(0?[1-9]|1[0-2])/([0-9]{4})");
	private static final Pattern JOUR_ISO = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
	private static final List<String> MOIS_FR = List.of("janv.", "févr.", "mars", "avr.", "mai", "juin",
			"juil.", "août", "sept.", "oct.", "nov.", "déc.");
	private static final List<String> MOIS_EN = List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun",
			"Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
	private static final List<String> MOIS_LONGS_FR = List.of("janvier", "février", "mars", "avril", "mai", "juin",
			"juillet", "août", "septembre", "octobre", "novembre", "décembre");
	private static final List<String> MOIS_LONGS_EN = List.of("January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December");

	private DatesCv() {
	}

	static boolean jourValide(String date) {
		return lireJour(date) != null;
	}

	static String convertirMoisAncien(String texte) {
		var date = MOIS_ANCIEN.matcher(ContenuCv.sansEspacesAuxBords(texte));
		if (!date.matches()) {
			return texte;
		}
		String mois = date.group(1);
		return date.group(2) + "-" + (mois.length() == 1 ? "0" + mois : mois);
	}

	static String formaterDate(String texte, boolean anglais) {
		var date = MOIS_ISO.matcher(texte);
		if (!date.matches()) {
			return texte;
		}
		int mois = Integer.parseInt(date.group(2));
		if (mois < 1 || mois > 12) {
			return date.group(1);
		}
		return (anglais ? MOIS_EN : MOIS_FR).get(mois - 1) + " " + date.group(1);
	}

	static String formaterJour(String texte, boolean anglais) {
		LocalDate date = lireJour(texte);
		if (date == null) {
			return texte;
		}
		int jour = date.getDayOfMonth();
		int mois = date.getMonthValue();
		String annee = texte.substring(0, 4);
		return anglais ? MOIS_LONGS_EN.get(mois - 1) + " " + jour + ", " + annee
				: (jour == 1 ? "1er" : jour) + " " + MOIS_LONGS_FR.get(mois - 1) + " " + annee;
	}

	static String periode(String debut, String fin, boolean enCours, boolean ouverte, boolean anglais) {
		String d = formaterDate(debut, anglais);
		String f = formaterDate(fin, anglais);
		if (d.isEmpty()) {
			return f;
		}
		if (enCours || (ouverte && f.isEmpty())) {
			return d + (anglais ? " – Present" : " – aujourd'hui");
		}
		return f.isEmpty() ? d : d + " – " + f;
	}

	private static LocalDate lireJour(String texte) {
		if (!JOUR_ISO.matcher(texte).matches()) {
			return null;
		}
		try {
			return LocalDate.parse(texte);
		} catch (DateTimeParseException erreur) {
			return null;
		}
	}
}
