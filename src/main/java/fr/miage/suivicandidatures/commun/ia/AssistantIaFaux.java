package fr.miage.suivicandidatures.commun.ia;

class AssistantIaFaux implements AssistantIa {

	@Override
	public String generer(String consignes, String demande) {
		return "Réponse de démonstration. Pour activer OpenAI, configurez IA_FOURNISSEUR=openai dans le fichier .env.";
	}
}
