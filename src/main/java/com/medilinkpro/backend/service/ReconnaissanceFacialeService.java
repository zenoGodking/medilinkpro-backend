package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.ConsultationMapper;
import com.medilinkpro.backend.dto.mapper.DossierMedicalMapper;
import com.medilinkpro.backend.dto.mapper.OrdonnanceMapper;
import com.medilinkpro.backend.dto.mapper.PatientMapper;
import com.medilinkpro.backend.dto.response.CandidatFacialResponse;
import com.medilinkpro.backend.dto.response.CarnetUrgenceResponse;
import com.medilinkpro.backend.dto.response.RechercheFacialeResponse;
import com.medilinkpro.backend.entity.AccesUrgenceLog;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.NiveauAccesUrgence;
import com.medilinkpro.backend.enums.NiveauConfiance;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.enums.TypeAccesCarnet;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.AccesUrgenceLogRepository;
import com.medilinkpro.backend.repository.ConsultationRepository;
import com.medilinkpro.backend.repository.OrdonnanceRepository;
import com.medilinkpro.backend.repository.PatientRepository;
import com.medilinkpro.backend.repository.ResultatAnalyseRepository;
import com.medilinkpro.backend.util.DescripteurFacial;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Recherche d'un patient par reconnaissance faciale (situation d'urgence : personne accidentee
 * inconsciente). L'empreinte faciale est calculee dans le navigateur (face-api) ; ce service
 * la compare aux empreintes enregistrees a l'inscription.
 *
 * Regles de securite :
 * - le resultat n'est JAMAIS une identification certaine : au plus 3 candidats, chacun avec
 *   un score de confiance et sa photo de reference pour confirmation visuelle ;
 * - seuls les candidats sous SEUIL_CORRESPONDANCE sont renvoyes, pour ne pas exposer les donnees
 *   de personnes sans rapport avec le visage scanne ;
 * - tout utilisateur connecte voit l'essentiel pour agir ; le personnel de sante valide voit
 *   le carnet complet ; aucune ecriture n'est possible par ce biais ;
 * - chaque recherche et chaque consultation de carnet est journalisee (AccesUrgenceLog).
 */
@Service
@RequiredArgsConstructor
public class ReconnaissanceFacialeService {

    /** Au-dela de cette distance, on considere que ce n'est pas la meme personne (seuil usuel face-api). */
    static final double SEUIL_CORRESPONDANCE = 0.6;
    static final double SEUIL_CONFIANCE_ELEVEE = 0.4;
    static final double SEUIL_CONFIANCE_MOYENNE = 0.5;
    /** Ecart minimal entre les deux meilleurs candidats pour qu'ils soient departageables. */
    static final double ECART_AMBIGUITE = 0.05;
    static final int NOMBRE_CANDIDATS = 3;

    private static final String DOSSIER_PHOTOS = "photos-faciales";
    private static final Set<Role> ROLES_PERSONNEL_SANTE = Set.of(Role.MEDECIN, Role.INFIRMIER);

    private static final String AVERTISSEMENT =
            "Correspondance probable uniquement : la reconnaissance faciale n'est pas fiable a 100 %. "
            + "Comparez la photo de reference avec la personne avant d'utiliser ces informations "
            + "et confirmez le groupe sanguin par un test avant toute transfusion.";

    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final ResultatAnalyseRepository resultatAnalyseRepository;
    private final OrdonnanceRepository ordonnanceRepository;
    private final AccesUrgenceLogRepository accesUrgenceLogRepository;
    private final FileStorageService fileStorageService;
    private final PatientMapper patientMapper;
    private final ConsultationMapper consultationMapper;
    private final DossierMedicalMapper dossierMedicalMapper;
    private final OrdonnanceMapper ordonnanceMapper;
    private final JournalAccesService journalAccesService;

    /**
     * Enregistre (ou remplace) la photo de reference et l'empreinte faciale d'un patient.
     * L'appelant est responsable de la sauvegarde du patient (entite geree dans la transaction).
     */
    public void enroler(Patient patient, MultipartFile photo, List<Double> descripteur) {
        double[] vecteur = DescripteurFacial.valider(descripteur);
        String anciennePhoto = patient.getPhotoFacialeChemin();

        patient.setPhotoFacialeChemin(fileStorageService.storePrivateImage(photo, DOSSIER_PHOTOS));
        patient.setDescripteurFacial(DescripteurFacial.serialiser(vecteur));

        fileStorageService.deletePrivate(anciennePhoto);
    }

    @Transactional
    public void enrolerPatientConnecte(Utilisateur utilisateur, MultipartFile photo, List<Double> descripteur) {
        Patient patient = patientRepository.findById(utilisateur.getId())
                .orElseThrow(() -> new BadRequestException("Seul un patient peut enregistrer sa photo faciale"));
        enroler(patient, photo, descripteur);
        patientRepository.save(patient);
    }

    @Transactional
    public RechercheFacialeResponse rechercher(List<Double> descripteur, Utilisateur demandeur) {
        double[] recherche = DescripteurFacial.valider(descripteur);
        boolean personnelSante = estPersonnelSanteValide(demandeur);

        List<Correspondance> meilleures = patientRepository.findAllAvecEmpreinteFaciale().stream()
                .map(p -> new Correspondance(p, DescripteurFacial.distance(
                        recherche, DescripteurFacial.deserialiser(p.getDescripteurFacial()))))
                .filter(c -> c.distance() < SEUIL_CORRESPONDANCE)
                .sorted(Comparator.comparingDouble(Correspondance::distance))
                .limit(NOMBRE_CANDIDATS)
                .toList();

        List<CandidatFacialResponse> candidats = new java.util.ArrayList<>();
        for (int i = 0; i < meilleures.size(); i++) {
            candidats.add(versCandidat(meilleures.get(i), i + 1, personnelSante));
        }

        boolean ambigu = meilleures.size() >= 2
                && meilleures.get(1).distance() - meilleures.get(0).distance() < ECART_AMBIGUITE;

        // Chaque candidat renvoye a vu ses donnees d'urgence montrees : il le verra dans son journal.
        meilleures.forEach(c -> journalAccesService.enregistrer(
                c.patient().getId(), demandeur, TypeAccesCarnet.RECONNAISSANCE_FACIALE));

        journaliser(demandeur, AccesUrgenceLog.TypeAcces.RECHERCHE_FACIALE,
                meilleures.isEmpty() ? null : meilleures.get(0).patient().getId(),
                meilleures.size(),
                meilleures.isEmpty() ? null : meilleures.get(0).distance());

        return RechercheFacialeResponse.builder()
                .niveauAcces(personnelSante ? NiveauAccesUrgence.COMPLET : NiveauAccesUrgence.ESSENTIEL)
                .candidats(candidats)
                .ambigu(ambigu)
                .avertissement(candidats.isEmpty()
                        ? "Aucune correspondance plausible. La personne n'est peut-etre pas inscrite, "
                          + "ou la photo est de trop mauvaise qualite (visage de face, bien eclaire)."
                        : AVERTISSEMENT)
                .build();
    }

    /** Carnet complet en lecture seule, reserve au personnel de sante valide. */
    @Transactional
    public CarnetUrgenceResponse carnetComplet(UUID patientId, Utilisateur demandeur) {
        if (!estPersonnelSanteValide(demandeur)) {
            throw new AccessDeniedException("Carnet complet reserve au personnel de sante valide");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient non trouve avec l'id : " + patientId));

        journaliser(demandeur, AccesUrgenceLog.TypeAcces.CARNET_COMPLET, patientId, null, null);
        journalAccesService.enregistrer(patientId, demandeur, TypeAccesCarnet.CARNET_URGENCE);

        var dossier = patient.getDossierMedical();
        return CarnetUrgenceResponse.builder()
                .patient(patientMapper.toResponse(patient))
                .photoReference(photoDataUrl(patient))
                .consultations(consultationRepository.findByPatientId(patientId).stream()
                        .map(consultationMapper::toResponse).toList())
                .resultatsAnalyses(dossier == null ? List.of()
                        : resultatAnalyseRepository.findByDossierMedicalId(dossier.getId()).stream()
                                .map(dossierMedicalMapper::toResponse).toList())
                .ordonnances(ordonnanceRepository.findByPatientId(patientId).stream()
                        .map(ordonnanceMapper::toResponse).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public String photoPatientConnecte(Utilisateur utilisateur) {
        Patient patient = patientRepository.findById(utilisateur.getId())
                .orElseThrow(() -> new BadRequestException("Seul un patient dispose d'une photo faciale"));
        return photoDataUrl(patient);
    }

    private CandidatFacialResponse versCandidat(Correspondance c, int rang, boolean personnelSante) {
        Patient p = c.patient();
        CandidatFacialResponse.CandidatFacialResponseBuilder builder = CandidatFacialResponse.builder()
                .rang(rang)
                .patientId(p.getId())
                .distance(Math.round(c.distance() * 1000) / 1000.0)
                .scoreConfiance(scoreConfiance(c.distance()))
                .niveauConfiance(niveauConfiance(c.distance()))
                .photoReference(photoDataUrl(p))
                .prenom(p.getPrenom())
                .groupeSanguin(p.getGroupeSanguin())
                .allergies(p.getAllergies())
                .conditionsUrgence(p.getConditionsUrgence())
                .decede(p.isDecede())
                .contactUrgenceNom(p.getContactUrgenceNom())
                .contactUrgenceTelephone(p.getContactUrgenceTelephone());
        if (personnelSante) {
            builder.nom(p.getNom()).dateNaissance(p.getDateNaissance());
        }
        return builder.build();
    }

    /**
     * Score indicatif 0-100 : 100 pour une distance <= 0.3 (quasi certainement la meme personne),
     * decroissant lineairement jusqu'a ~14 au seuil de correspondance (0.6).
     */
    static int scoreConfiance(double distance) {
        double score = (0.65 - distance) / 0.35;
        return (int) Math.round(Math.max(0, Math.min(1, score)) * 100);
    }

    static NiveauConfiance niveauConfiance(double distance) {
        if (distance < SEUIL_CONFIANCE_ELEVEE) return NiveauConfiance.ELEVEE;
        if (distance < SEUIL_CONFIANCE_MOYENNE) return NiveauConfiance.MOYENNE;
        return NiveauConfiance.FAIBLE;
    }

    private boolean estPersonnelSanteValide(Utilisateur u) {
        return ROLES_PERSONNEL_SANTE.contains(u.getRole())
                && u.getStatutCompte() == StatutCompte.APPROUVE
                && u.isActif();
    }

    private String photoDataUrl(Patient patient) {
        String chemin = patient.getPhotoFacialeChemin();
        if (chemin == null) {
            return null;
        }
        String extension = chemin.substring(chemin.lastIndexOf('.') + 1).toLowerCase();
        String mime = switch (extension) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
        try {
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(fileStorageService.readPrivate(chemin));
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    private void journaliser(Utilisateur u, AccesUrgenceLog.TypeAcces type, UUID patientId,
                             Integer nombreCandidats, Double meilleureDistance) {
        accesUrgenceLogRepository.save(AccesUrgenceLog.builder()
                .utilisateurId(u.getId())
                .utilisateurRole(u.getRole().name())
                .typeAcces(type)
                .patientId(patientId)
                .nombreCandidats(nombreCandidats)
                .meilleureDistance(meilleureDistance)
                .build());
    }

    private record Correspondance(Patient patient, double distance) {
    }
}
