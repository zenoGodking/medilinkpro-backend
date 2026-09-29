package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.ConsultationMapper;
import com.medilinkpro.backend.dto.mapper.DossierMedicalMapper;
import com.medilinkpro.backend.dto.mapper.OrdonnanceMapper;
import com.medilinkpro.backend.dto.mapper.PatientMapper;
import com.medilinkpro.backend.dto.request.DeclarationDecesRequest;
import com.medilinkpro.backend.dto.response.AutorisationResponse;
import com.medilinkpro.backend.dto.response.CarnetResponse;
import com.medilinkpro.backend.dto.response.DecesResponse;
import com.medilinkpro.backend.dto.response.PatientAccessibleResponse;
import com.medilinkpro.backend.entity.AutorisationEcriture;
import com.medilinkpro.backend.entity.Consultation;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.NotificationSms;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.MotifEcriture;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutAlerte;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.enums.TypeAccesCarnet;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.AlerteSoinDomicileRepository;
import com.medilinkpro.backend.repository.AutorisationEcritureRepository;
import com.medilinkpro.backend.repository.ConsultationRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import com.medilinkpro.backend.repository.OrdonnanceRepository;
import com.medilinkpro.backend.repository.PatientRepository;
import com.medilinkpro.backend.repository.RendezVousRepository;
import com.medilinkpro.backend.repository.ResultatAnalyseRepository;
import com.medilinkpro.backend.service.sms.NotificationSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Regles d'acces au carnet medical d'un patient :
 * - LECTURE : le patient lui-meme, tout medecin valide, l'administrateur. Les autres utilisateurs
 *   (dont les autres patients) n'ont acces qu'aux donnees d'urgence via la reconnaissance faciale ;
 * - ECRITURE : uniquement un medecin valide, et seulement si le patient l'a autorise
 *   (AutorisationEcriture) ou s'il s'agit de son ancien patient (consultation, ou rendez-vous
 *   confirme/termine) ;
 * - EXCEPTION : tout medecin valide peut declarer le deces d'un patient ; son proche est alors informe.
 */
@Service
@RequiredArgsConstructor
public class CarnetAccesService {

    private static final Set<StatutRendezVous> RDV_ANCIEN_PATIENT = Set.of(StatutRendezVous.CONFIRME, StatutRendezVous.TERMINE);
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final AutorisationEcritureRepository autorisationRepository;
    private final ConsultationRepository consultationRepository;
    private final RendezVousRepository rendezVousRepository;
    private final ResultatAnalyseRepository resultatAnalyseRepository;
    private final OrdonnanceRepository ordonnanceRepository;
    private final AlerteSoinDomicileRepository alerteRepository;
    private final NotificationSmsService notificationSmsService;
    private final JournalAccesService journalAccesService;
    private final PatientMapper patientMapper;
    private final ConsultationMapper consultationMapper;
    private final DossierMedicalMapper dossierMedicalMapper;
    private final OrdonnanceMapper ordonnanceMapper;

    // ------------------------------------------------------------------ Regles

    public static boolean estMedecinValide(Utilisateur u) {
        return u.getRole() == Role.MEDECIN && u.getStatutCompte() == StatutCompte.APPROUVE && u.isActif();
    }

    /** Lecture du carnet : le patient lui-meme, un medecin valide ou un administrateur. */
    public void verifierLecture(Utilisateur u, UUID patientId) {
        boolean autorise = u.getRole() == Role.ADMIN
                || estMedecinValide(u)
                || (u.getRole() == Role.PATIENT && u.getId().equals(patientId));
        if (!autorise) {
            throw new AccessDeniedException("Vous n'avez pas acces a ce carnet medical");
        }
        journalAccesService.enregistrer(patientId, u, TypeAccesCarnet.LECTURE);
    }

    /** Lecture d'une liste globale (tous les patients, toutes les consultations...). */
    public void verifierLectureGlobale(Utilisateur u) {
        if (u.getRole() != Role.ADMIN && !estMedecinValide(u)) {
            throw new AccessDeniedException("Reserve aux medecins et aux administrateurs");
        }
    }

    @Transactional(readOnly = true)
    public Optional<MotifEcriture> motifEcriture(UUID medecinId, UUID patientId) {
        if (autorisationRepository.existsByPatientIdAndMedecinIdAndDateRevocationIsNull(patientId, medecinId)) {
            return Optional.of(MotifEcriture.AUTORISATION_PATIENT);
        }
        if (consultationRepository.existsByPatientIdAndMedecinId(patientId, medecinId)
                || rendezVousRepository.existsByPatientIdAndMedecinIdAndStatutIn(patientId, medecinId, RDV_ANCIEN_PATIENT)) {
            return Optional.of(MotifEcriture.ANCIEN_PATIENT);
        }
        return Optional.empty();
    }

    /** Ecriture dans le carnet : medecin valide, autorise par le patient ou ancien patient. */
    @Transactional(readOnly = true)
    public void verifierEcriture(Utilisateur u, UUID patientId) {
        if (!estMedecinValide(u)) {
            throw new AccessDeniedException("Seul un medecin peut ecrire dans un carnet medical");
        }
        Patient patient = getPatient(patientId);
        if (patient.isDecede()) {
            throw new BadRequestException("Ce patient est declare decede : son carnet est clos");
        }
        if (motifEcriture(u.getId(), patientId).isEmpty()) {
            throw new AccessDeniedException(
                    "Vous pouvez consulter ce carnet mais pas y ecrire : le patient doit d'abord vous y autoriser.");
        }
        journalAccesService.enregistrer(patientId, u, TypeAccesCarnet.ECRITURE);
    }

    // ------------------------------------------------------------------ Carnet

    @Transactional(readOnly = true)
    public CarnetResponse carnet(UUID patientId, Utilisateur u) {
        verifierLecture(u, patientId);
        Patient patient = getPatient(patientId);
        Optional<MotifEcriture> motif = estMedecinValide(u) && !patient.isDecede()
                ? motifEcriture(u.getId(), patientId)
                : Optional.empty();

        var dossier = patient.getDossierMedical();
        return CarnetResponse.builder()
                .patient(patientMapper.toResponse(patient))
                .consultations(consultationRepository.findByPatientId(patientId).stream()
                        .map(consultationMapper::toResponse).toList())
                .resultatsAnalyses(dossier == null ? List.of()
                        : resultatAnalyseRepository.findByDossierMedicalId(dossier.getId()).stream()
                                .map(dossierMedicalMapper::toResponse).toList())
                .ordonnances(ordonnanceRepository.findByPatientId(patientId).stream()
                        .map(ordonnanceMapper::toResponse).toList())
                .ecritureAutorisee(motif.isPresent())
                .motifEcriture(motif.orElse(null))
                .build();
    }

    /** Patients dans le carnet desquels le medecin peut ecrire (autorisations + anciens patients). */
    @Transactional(readOnly = true)
    public List<PatientAccessibleResponse> patientsAccessiblesEnEcriture(Utilisateur medecin) {
        if (!estMedecinValide(medecin)) {
            throw new AccessDeniedException("Reserve aux medecins");
        }
        Map<UUID, PatientAccessibleResponse> resultat = new LinkedHashMap<>();
        autorisationRepository.findByMedecinIdAndDateRevocationIsNull(medecin.getId())
                .forEach(a -> ajouter(resultat, a.getPatient(), MotifEcriture.AUTORISATION_PATIENT));
        consultationRepository.findByMedecinId(medecin.getId()).stream()
                .map(Consultation::getPatient)
                .forEach(p -> ajouter(resultat, p, MotifEcriture.ANCIEN_PATIENT));
        rendezVousRepository.findByMedecinId(medecin.getId()).stream()
                .filter(r -> RDV_ANCIEN_PATIENT.contains(r.getStatut()))
                .map(RendezVous::getPatient)
                .forEach(p -> ajouter(resultat, p, MotifEcriture.ANCIEN_PATIENT));
        return List.copyOf(resultat.values());
    }

    private void ajouter(Map<UUID, PatientAccessibleResponse> resultat, Patient p, MotifEcriture motif) {
        if (p.isDecede()) {
            return;
        }
        resultat.putIfAbsent(p.getId(), PatientAccessibleResponse.builder()
                .id(p.getId()).nom(p.getNom()).prenom(p.getPrenom()).motif(motif).build());
    }

    // ------------------------------------------------------------------ Autorisations (patient)

    @Transactional(readOnly = true)
    public List<AutorisationResponse> mesAutorisations(Utilisateur patient) {
        verifierPatient(patient);
        return autorisationRepository.findByPatientIdAndDateRevocationIsNullOrderByDateAutorisationDesc(patient.getId())
                .stream().map(this::toAutorisation).toList();
    }

    @Transactional
    public AutorisationResponse autoriser(Utilisateur utilisateur, UUID medecinId) {
        verifierPatient(utilisateur);
        Medecin medecin = medecinRepository.findById(medecinId)
                .orElseThrow(() -> new ResourceNotFoundException("Medecin non trouve"));
        if (!estMedecinValide(medecin)) {
            throw new BadRequestException("Ce medecin n'est pas (encore) valide sur la plateforme");
        }
        return autorisationRepository.findFirstByPatientIdAndMedecinIdAndDateRevocationIsNull(utilisateur.getId(), medecinId)
                .map(this::toAutorisation)
                .orElseGet(() -> toAutorisation(autorisationRepository.save(AutorisationEcriture.builder()
                        .patient(getPatient(utilisateur.getId()))
                        .medecin(medecin)
                        .build())));
    }

    @Transactional
    public void revoquer(Utilisateur utilisateur, UUID medecinId) {
        verifierPatient(utilisateur);
        autorisationRepository.findFirstByPatientIdAndMedecinIdAndDateRevocationIsNull(utilisateur.getId(), medecinId)
                .ifPresent(a -> a.setDateRevocation(LocalDateTime.now()));
    }

    private void verifierPatient(Utilisateur u) {
        if (u.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Reserve au patient");
        }
    }

    private AutorisationResponse toAutorisation(AutorisationEcriture a) {
        Medecin m = a.getMedecin();
        return AutorisationResponse.builder()
                .medecinId(m.getId())
                .medecinNom(m.getNom())
                .medecinPrenom(m.getPrenom())
                .specialite(m.getSpecialite())
                .dateAutorisation(a.getDateAutorisation())
                .build();
    }

    // ------------------------------------------------------------------ Deces

    /**
     * Tout medecin valide peut declarer le deces d'un patient (meme sans droit d'ecriture) :
     * le compte est desactive, ses alertes en attente sont annulees et son proche est informe par SMS.
     */
    @Transactional
    public DecesResponse declarerDeces(UUID patientId, Utilisateur declarant, DeclarationDecesRequest request) {
        if (!estMedecinValide(declarant)) {
            throw new AccessDeniedException("Seul un medecin peut declarer un deces");
        }
        Patient patient = getPatient(patientId);
        if (patient.isDecede()) {
            throw new BadRequestException("Ce patient est deja declare decede");
        }

        patient.setDecede(true);
        patient.setDateDeces(request.getDateDeces());
        patient.setCirconstancesDeces(request.getCirconstances());
        patient.setDecesDeclarePar(declarant.getId());
        patient.setDateDeclarationDeces(LocalDateTime.now());
        patientRepository.save(patient);
        journalAccesService.enregistrer(patientId, declarant, TypeAccesCarnet.DECLARATION_DECES);

        alerteRepository.findByPatientIdOrderByDateCreationDesc(patientId).stream()
                .filter(a -> a.getStatut() == StatutAlerte.EN_ATTENTE)
                .forEach(a -> a.setStatut(StatutAlerte.ANNULEE));

        String numero = patient.getContactUrgenceTelephone();
        NotificationSms.Statut statut = null;
        if (numero != null && !numero.isBlank()) {
            statut = notificationSmsService.envoyer(numero, messageDeces(patient, declarant), "DECES", patientId).getStatut();
        }

        return DecesResponse.builder()
                .patient(patientMapper.toResponse(patient))
                .procheTelephone(numero)
                .statutNotification(statut)
                .build();
    }

    /** Annulation d'une declaration erronee, reservee a l'administrateur. */
    @Transactional
    public void annulerDeces(UUID patientId, Utilisateur admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Reserve a l'administrateur");
        }
        Patient patient = getPatient(patientId);
        patient.setDecede(false);
        patient.setDateDeces(null);
        patient.setCirconstancesDeces(null);
        patient.setDecesDeclarePar(null);
        patient.setDateDeclarationDeces(null);
    }

    private String messageDeces(Patient patient, Utilisateur medecin) {
        String contact = medecin.getTelephone() != null ? " (" + medecin.getTelephone() + ")" : "";
        return "MediLinkPro : nous avons le regret de vous informer du deces de "
                + patient.getPrenom() + " " + patient.getNom()
                + ", survenu le " + patient.getDateDeces().format(FORMAT_DATE)
                + ". Deces constate par le Dr " + medecin.getPrenom() + " " + medecin.getNom() + contact
                + ". Toutes nos condoleances.";
    }

    private Patient getPatient(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient non trouve avec l'id : " + id));
    }
}
