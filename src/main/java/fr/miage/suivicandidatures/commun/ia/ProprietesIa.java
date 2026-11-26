package fr.miage.suivicandidatures.commun.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ia")
record ProprietesIa(String fournisseur, String modele, String cle, String url) {

	@Override
	public String toString() {
		return "ProprietesIa[fournisseur=" + fournisseur + ", modele=" + modele + "]";
	}
}
