package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.SignalTeleconsultationRequest;
import com.medilinkpro.backend.dto.response.SignalTeleconsultationResponse;
import com.medilinkpro.backend.dto.response.TeleconsultationResponse;
import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.enums.TypeConsultation;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

/**
 * Teleconsultation video entre le medecin et le patient d'un rendez-vous de type TELECONSULTATION.
 * La video circule en pair-a-pair (WebRTC) ; le serveur ne fait que relayer la signalisation
 * (offre/reponse SDP, candidats ICE, chat) d'un participant a l'autre via /user/queue/teleconsultation,
 * et seulement pendant la fenetre d'ouverture de la salle autour de l'heure du rendez-vous.
 */
@Service
@RequiredArgsConstructor
public class TeleconsultationService {

    public static final String QUEUE = "/queue/teleconsultation";
    private static final Set<String> TYPES_AUTORISES =
            Set.of("join", "ready", "offer", "answer", "ice", "leave", "chat", "media", "ordonnance");
    private static final int TAILLE_MAX_CHAT = 2000;

    private final RendezVousRepository rendezVousRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CarnetAccesService carnetAccesService;
    private final ConsultationService consultationService;
    private final OrdonnanceService ordonnanceService;
    private final com.medilinkpro.backend.repository.ConsultationRepository consultationRepository;
    private final NotificationService notificationService;

    @Value("${medilinkpro.fuseau-horaire:Africa/Douala}")
    private String fuseau;

    @Value("${medilinkpro.teleconsultation.ouverture-minutes-avant:15}")
    private long ouvertureMinutesAvant;

    @Value("${medilinkpro.teleconsultation.duree-max-minutes:120}")
    private long dureeMaxMinutes;

    @Transactional(readOnly = true)
    public TeleconsultationResponse infos(UUID rendezVousId, Utilisateur u) {
        RendezVous rdv = getRendezVous(rendezVousId);
        String role = roleDans(rdv, u);
        LocalDateTime ouverture = rdv.getDateHeure().minusMinutes(ouvertureMinutesAvant);
        LocalDateTime fermeture = rdv.getDateHeure().plusMinutes(dureeMaxMinutes);
        String raison = raisonFermeture(rdv, ouverture, fermeture);

        return TeleconsultationResponse.builder()
                .rendezVousId(rdv.getId())
                .dateHeure(rdv.getDateHeure())
                .statut(rdv.getStatut())
                .medecinId(rdv.getMedecin().getId())
                .medecinNomComplet(rdv.getMedecin().getPrenom() + " " + rdv.getMedecin().getNom())
                .specialiteMedecin(rdv.getMedecin().getSpecialite())
                .patientId(rdv.getPatient().getId())
                .patientNomComplet(rdv.getPatient().getPrenom() + " " + rdv.getPatient().getNom())
                .monRole(role)
                .ouvertureA(ouverture)
                .fermetureA(fermeture)
                .ouverte(raison == null)
                .raison(raison)
                .build();
    }

    /** Relaie un message de signalisation a l'autre participant, si la salle est ouverte. */
    @Transactional(readOnly = true)
    public void relayer(UUID rendezVousId, Utilisateur expediteur, SignalTeleconsultationRequest signal) {
        if (signal.getType() == null || !TYPES_AUTORISES.contains(signal.getType())) {
            throw new BadRequestException("Type de message inconnu");
        }
        if ("chat".equals(signal.getType()) && signal.getData() != null
                && signal.getData().path("texte").asText("").length() > TAILLE_MAX_CHAT) {
            throw new BadRequestException("Message trop long");
        }
        RendezVous rdv = getRendezVous(rendezVousId);
        String role = roleDans(rdv, expediteur);
        String raison = raisonFermeture(rdv, rdv.getDateHeure().minusMinutes(ouvertureMinutesAvant),
                rdv.getDateHeure().plusMinutes(dureeMaxMinutes));
        if (raison != null) {
            throw new BadRequestException(raison);
        }

        Utilisateur destinataire = "MEDECIN".equals(role) ? rdv.getPatient() : rdv.getMedecin();
        messagingTemplate.convertAndSendToUser(destinataire.getEmail(), QUEUE, SignalTeleconsultationResponse.builder()
                .rendezVousId(rdv.getId())
                .de(expediteur.getId())
                .deRole(role)
                .type(signal.getType())
                .data(signal.getData())
                .build());
    }

    /**
     * Fin de teleconsultation par le medecin : enregistre la consultation (compte rendu) dans le carnet,
     * emet l'ordonnance si des medicaments sont prescrits, cloture le rendez-vous et previent le patient.
     * Une seule cloture par rendez-vous.
     */
    @Transactional
    public com.medilinkpro.backend.dto.response.ClotureTeleconsultationResponse cloturer(
            UUID rendezVousId, Utilisateur medecin, com.medilinkpro.backend.dto.request.ClotureTeleconsultationRequest request) {
        RendezVous rdv = getRendezVous(rendezVousId);
        if (!rdv.getMedecin().getId().equals(medecin.getId())) {
            throw new AccessDeniedException("Seul le médecin de ce rendez-vous peut le clôturer");
        }
        if (rdv.getType() != TypeConsultation.TELECONSULTATION) {
            throw new BadRequestException("Ce rendez-vous n'est pas une téléconsultation");
        }
        if (rdv.getStatut() != StatutRendezVous.CONFIRME && rdv.getStatut() != StatutRendezVous.TERMINE) {
            throw new BadRequestException("Seul un rendez-vous confirmé peut être clôturé");
        }
        if (rdv.getConsultation() != null) {
            throw new com.medilinkpro.backend.exception.ConflictException("Le compte rendu de cette téléconsultation a déjà été enregistré");
        }
        carnetAccesService.verifierEcriture(medecin, rdv.getPatient().getId());

        var consultation = consultationService.create(com.medilinkpro.backend.dto.request.ConsultationRequest.builder()
                .patientId(rdv.getPatient().getId())
                .medecinId(medecin.getId())
                .date(LocalDateTime.now(Clock.system(ZoneId.of(fuseau))))
                .motif(request.getMotif())
                .diagnostic(request.getDiagnostic())
                .compteRendu(request.getCompteRendu())
                .typeConsultation(TypeConsultation.TELECONSULTATION)
                .build());
        com.medilinkpro.backend.dto.response.OrdonnanceResponse ordonnance = null;
        if (request.getMedicaments() != null && !request.getMedicaments().isBlank()) {
            ordonnance = ordonnanceService.create(com.medilinkpro.backend.dto.request.OrdonnanceRequest.builder()
                    .consultationId(consultation.getId())
                    .medicaments(request.getMedicaments().trim())
                    .posologie(request.getPosologie())
                    .build());
        }
        rdv.setConsultation(consultationRepository.getReferenceById(consultation.getId()));
        rdv.setStatut(StatutRendezVous.TERMINE);

        notificationService.notifier(rdv.getPatient(),
                ordonnance != null ? "Votre ordonnance est disponible" : "Compte rendu de téléconsultation",
                "Le Dr " + rdv.getMedecin().getPrenom() + " " + rdv.getMedecin().getNom() + " a enregistré le compte rendu de votre téléconsultation"
                        + (ordonnance != null ? " et vous a prescrit une ordonnance, à présenter en pharmacie avec son QR code." : "."),
                "/patient/dossier", true);
        return com.medilinkpro.backend.dto.response.ClotureTeleconsultationResponse.builder()
                .consultation(consultation).ordonnance(ordonnance).build();
    }

    private String raisonFermeture(RendezVous rdv, LocalDateTime ouverture, LocalDateTime fermeture) {
        if (rdv.getType() != TypeConsultation.TELECONSULTATION) {
            return "Ce rendez-vous n'est pas une téléconsultation";
        }
        if (rdv.getStatut() != StatutRendezVous.CONFIRME) {
            return "La téléconsultation n'est possible que pour un rendez-vous confirmé";
        }
        LocalDateTime maintenant = LocalDateTime.now(Clock.system(ZoneId.of(fuseau)));
        if (maintenant.isBefore(ouverture)) {
            return "La salle ouvrira " + ouvertureMinutesAvant + " minutes avant le rendez-vous";
        }
        if (maintenant.isAfter(fermeture)) {
            return "Le créneau de cette téléconsultation est passé";
        }
        return null;
    }

    private static String roleDans(RendezVous rdv, Utilisateur u) {
        if (rdv.getMedecin().getId().equals(u.getId())) {
            return "MEDECIN";
        }
        if (rdv.getPatient().getId().equals(u.getId())) {
            return "PATIENT";
        }
        throw new AccessDeniedException("Ce rendez-vous ne vous concerne pas");
    }

    private RendezVous getRendezVous(UUID id) {
        return rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous non trouvé avec l'id : " + id));
    }
}
