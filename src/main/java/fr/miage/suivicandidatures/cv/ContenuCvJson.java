package fr.miage.suivicandidatures.cv;

import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Converter
public class ContenuCvJson implements AttributeConverter<ContenuCv, String> {

	private static final JsonMapper JSON = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	@Override
	public String convertToDatabaseColumn(ContenuCv contenu) {
		return contenu == null ? null : JSON.writeValueAsString(contenu);
	}

	@Override
	public ContenuCv convertToEntityAttribute(String document) {
		if (document == null) {
			return null;
		}
		ObjectNode contenu = (ObjectNode) JSON.readTree(document);
		ObjectNode identite = contenu.withObjectProperty("identite");
		if (!identite.has("prenom") || identite.has("lien") || identite.has("profil")) {
			normaliserAncienContenu(contenu, identite);
		}
		return JSON.treeToValue(contenu, ContenuCv.class);
	}

	private static void normaliserAncienContenu(ObjectNode contenu, ObjectNode identite) {
		completerTextes(identite, "prenom", "nom", "titre", "ville", "email", "telephone");
		tableau(identite, "permis");
		ArrayNode liens = tableau(identite, "liens");
		String lien = identite.path("lien").asString("");
		if (liens.isEmpty() && !ContenuCv.sansEspacesAuxBords(lien).isEmpty()) {
			liens.addObject().put("libelle", "Lien").put("url", lien);
		}
		if (!contenu.hasNonNull("profil")) {
			contenu.put("profil", identite.path("profil").asString(""));
		}
		identite.remove(List.of("lien", "profil"));

		for (JsonNode experience : tableau(contenu, "experiences")) {
			ObjectNode objet = (ObjectNode) experience;
			completerTextes(objet, "poste", "entreprise", "lieu", "description");
			completerPeriode(objet);
			tableau(objet, "realisations");
		}
		for (JsonNode formation : tableau(contenu, "formations")) {
			ObjectNode objet = (ObjectNode) formation;
			if (!objet.has("etablissement")) {
				objet.put("etablissement", objet.path("ecole").asString(""));
			}
			objet.remove("ecole");
			completerTextes(objet, "diplome", "mention", "description");
			completerPeriode(objet);
		}
		for (JsonNode groupe : tableau(contenu, "competences")) {
			ObjectNode objet = (ObjectNode) groupe;
			completerTextes(objet, "groupe");
			tableau(objet, "elements");
		}
		for (String champ : List.of("savoirEtre", "centresInteret", "projets", "langues", "certifications")) {
			tableau(contenu, champ);
		}
		ObjectNode disponibilite = contenu.withObjectProperty("disponibilite");
		completerTextes(disponibilite, "date");
		if (!disponibilite.hasNonNull("immediate")) {
			disponibilite.put("immediate", false);
		}
	}

	private static void completerPeriode(ObjectNode objet) {
		completerTextes(objet, "debut", "fin");
		if (!objet.hasNonNull("enCours")) {
			objet.put("enCours", false);
		}
		for (String champ : List.of("debut", "fin")) {
			if (objet.hasNonNull(champ)) {
				objet.put(champ, DatesCv.convertirMoisAncien(objet.path(champ).asString()));
			}
		}
	}

	private static ArrayNode tableau(ObjectNode objet, String champ) {
		return objet.path(champ) instanceof ArrayNode liste ? liste : objet.putArray(champ);
	}

	private static void completerTextes(ObjectNode objet, String... champs) {
		for (String champ : champs) {
			if (!objet.has(champ)) {
				objet.put(champ, "");
			}
		}
	}
}
