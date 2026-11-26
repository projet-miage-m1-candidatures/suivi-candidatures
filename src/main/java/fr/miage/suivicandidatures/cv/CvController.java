package fr.miage.suivicandidatures.cv;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import fr.miage.suivicandidatures.commun.CandidatCourant;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(CvController.CHEMIN)
class CvController {

	static final String CHEMIN = "/api/v1/cv";

	private final CvService service;
	private final CandidatCourant candidatCourant;
	private final CvPdf pdf;

	CvController(CvService service, CandidatCourant candidatCourant, CvPdf pdf) {
		this.service = service;
		this.candidatCourant = candidatCourant;
		this.pdf = pdf;
	}

	@GetMapping
	List<ReponseCv> lister() {
		return service.lister(candidatCourant.id()).stream().map(ReponseCv::depuis).toList();
	}

	@PostMapping
	ResponseEntity<ReponseCv> creer(@Valid @RequestBody DemandeCv demande) {
		return reponseCreation(service.creer(candidatCourant.id(), demande));
	}

	@GetMapping("/{id}")
	ReponseCv lire(@PathVariable Long id) {
		return ReponseCv.depuis(service.lire(candidatCourant.id(), id));
	}

	@GetMapping("/{id}/pdf")
	ResponseEntity<byte[]> telechargerPdf(@PathVariable Long id) {
		Cv cv = service.lire(candidatCourant.id(), id);
		ContentDisposition disposition = ContentDisposition.attachment().filename(CvPdf.nomFichier(cv)).build();
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.body(pdf.generer(cv));
	}

	@PutMapping("/{id}")
	ReponseCv enregistrer(@PathVariable Long id, @Valid @RequestBody DemandeCv demande) {
		return ReponseCv.depuis(service.enregistrer(candidatCourant.id(), id, demande));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void supprimer(@PathVariable Long id) {
		service.supprimer(candidatCourant.id(), id);
	}

	@PostMapping("/{id}/copie")
	ResponseEntity<ReponseCv> dupliquer(@PathVariable Long id) {
		return reponseCreation(service.dupliquer(candidatCourant.id(), id));
	}

	@ExceptionHandler(ResponseStatusException.class)
	ProblemDetail erreurMetier(ResponseStatusException erreur) {
		return erreur.getBody();
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail demandeInvalide(MethodArgumentNotValidException erreur) {
		String detail = erreur.getBindingResult().getFieldErrors().stream()
				.map(champ -> champ.getDefaultMessage())
				.map(message -> message.endsWith(".") ? message : message + ".")
				.distinct()
				.sorted()
				.collect(Collectors.joining(" "));
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail corpsInvalide() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Le corps doit contenir un CV JSON valide avec une langue, un modèle et une police autorisés.");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail identifiantInvalide() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "L'identifiant du CV doit être un entier.");
	}

	private static ResponseEntity<ReponseCv> reponseCreation(Cv cv) {
		return ResponseEntity.created(URI.create(CHEMIN + "/" + cv.getId())).body(ReponseCv.depuis(cv));
	}

	record ReponseCv(Long id, String titre, LangueCv langue, ModeleCv modele, PoliceCv police,
			Instant dateCreation, Instant dateModification, ContenuCv contenu) {

		static ReponseCv depuis(Cv cv) {
			return new ReponseCv(cv.getId(), cv.getTitre(), cv.getLangue(), cv.getModele(), cv.getPolice(),
					cv.getDateCreation(), cv.getDateModification(), cv.getContenu());
		}
	}
}
