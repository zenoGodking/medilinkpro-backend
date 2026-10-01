package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.RendezVousMapper;
import com.medilinkpro.backend.dto.request.AvisRequest;
import com.medilinkpro.backend.dto.request.DecisionRendezVousRequest;
import com.medilinkpro.backend.dto.request.RendezVousRequest;
import com.medilinkpro.backend.dto.request.StatutRendezVousRequest;
import com.medilinkpro.backend.dto.response.AvisMedecinResumeResponse;
import com.medilinkpro.backend.dto.response.AvisResponse;
import com.medilinkpro.backend.dto.response.RendezVousResponse;
import com.medilinkpro.backend.entity.AvisMedecin;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.enums.TypeConsultation;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ConflictException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.AvisMedecinRepository;
import com.medilinkpro.backend.repository.EtablissementRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import com.medilinkpro.backend.repository.PatientRepository;
import com.medilinkpro.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Rendez-vous (Module 2) :
 * - le patient demande un creneau du calendrier du medecin -> EN_ATTENTE, le medecin est prevenu ;
 * - le medecin accepte (CONFIRME), refuse (REFUSE) ou reporte sur un autre creneau libre (CONFIRME,
 *   heure initiale conservee) ; le patient est prevenu a chaque decision (application, push, SMS) ;
 * - le patient peut annuler ; le medecin est alors prevenu ;
 * - apres un rendez-vous effectue, le patient peut laisser un avis sur le medecin.
 */
@Service
@RequiredArgsConstructor
public class RendezVousService {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM 'à' HH'h'mm", Locale.FRENCH);
    private static final Set<StatutRendezVous> STATUTS_MEDECIN_LIBRES = Set.of(StatutRendezVous.TERMINE, StatutRendezVous.NO_SHOW);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RendezVousRepository rendezVousRepository;
    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final EtablissementRepository etablissementRepository;
    private final RendezVousMapper rendezVousMapper;
    private final DisponibiliteService disponibiliteService;
    private final NotificationService notificationService;
    private final AvisMedecinRepository avisRepository;

    @Value("${medilinkpro.fuseau-horaire:Africa/Douala}")
    private String fuseau;

    // ------------------------------------------------------------------ Lecture

    @Transactional(readOnly = true)
    public List<RendezVousResponse> findAll() {
        return rendezVousRepository.findAll().stream().map(rendezVousMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RendezVousResponse findById(UUID id) {
        return rendezVousMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<RendezVousResponse> findByPatient(UUID patientId) {
        Set<UUID> notes = new HashSet<>(avisRepository.rendezVousNotes(patientId));
        return rendezVousRepository.findByPatientId(patientId).stream()
                .map(r -> {
                    RendezVousResponse response = rendezVousMapper.toResponse(r);
                    response.setAvisDonne(notes.contains(r.getId()));
                    return response;
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<RendezVousResponse> findByMedecin(UUID medecinId) {
        return rendezVousRepository.findByMedecinId(medecinId).stream().map(rendezVousMapper::toResponse).toList();
    }

    // ------------------------------------------------------------------ Demande (patient)

    @Transactional
    public RendezVousResponse create(RendezVousRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient non trouvé avec l'id : " + request.getPatientId()));
        Medecin medecin = medecinRepository.findById(request.getMedecinId())
                .orElseThrow(() -> new ResourceNotFoundException("Médecin non trouvé avec l'id : " + request.getMedecinId()));

        // L'heure doit tomber sur un creneau du calendrier du medecin (heures de consultation, hors absences)
        disponibiliteService.verifierCreneau(medecin.getId(), request.getDateHeure());
        if (rendezVousRepository.existsCreneauOccupe(medecin.getId(), request.getDateHeure())) {
            throw new BadRequestException(
                    "Ce créneau n'est plus disponible pour ce médecin. Veuillez choisir un autre horaire.");
        }

        EtablissementSante etablissement = null;
        if (request.getEtablissementId() != null) {
            etablissement = etablissementRepository.findById(request.getEtablissementId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Établissement non trouvé avec l'id : " + request.getEtablissementId()));
        }

        RendezVous rdv = rendezVousRepository.save(RendezVous.builder()
                .patient(patient)
                .medecin(medecin)
                .etablissement(etablissement)
                .dateHeure(request.getDateHeure())
                .type(request.getType())
                .statut(StatutRendezVous.EN_ATTENTE)
                .rappelEnvoye(false)
                .codeConfirmation(genererCodeConfirmation())
                .build());

        notificationService.notifier(medecin, "Nouvelle demande de rendez-vous",
                patient.getPrenom() + " " + patient.getNom() + " demande un rendez-vous " + libelleType(rdv)
                        + " le " + formater(rdv.getDateHeure()) + ". Acceptez, reportez ou refusez depuis votre agenda.",
                "/medecin/agenda", false);
        return rendezVousMapper.toResponse(rdv);
    }

    // ------------------------------------------------------------------ Decisions du medecin

    @Transactional
    public RendezVousResponse accepter(UUID id, Utilisateur medecin) {
        RendezVous rdv = getPourMedecin(id, medecin);
        if (rdv.getStatut() != StatutRendezVous.EN_ATTENTE) {
            throw new ConflictException("Seule une demande en attente peut être acceptée");
        }
        rdv.setStatut(StatutRendezVous.CONFIRME);
        notificationService.notifier(rdv.getPatient(), "Rendez-vous confirmé",
                "Le Dr " + nomMedecin(rdv) + " a confirmé votre rendez-vous " + libelleType(rdv) + " du "
                        + formater(rdv.getDateHeure()) + ". Code : " + rdv.getCodeConfirmation() + ".",
                "/patient/rendez-vous", true);
        return rendezVousMapper.toResponse(rdv);
    }

    @Transactional
    public RendezVousResponse refuser(UUID id, Utilisateur medecin, DecisionRendezVousRequest decision) {
        RendezVous rdv = getPourMedecin(id, medecin);
        if (rdv.getStatut() != StatutRendezVous.EN_ATTENTE && rdv.getStatut() != StatutRendezVous.CONFIRME) {
            throw new ConflictException("Ce rendez-vous ne peut plus être refusé");
        }
        rdv.setStatut(StatutRendezVous.REFUSE);
        rdv.setMotifMedecin(nettoyer(decision.getMotif()));
        notificationService.notifier(rdv.getPatient(), "Rendez-vous non disponible",
                "Le Dr " + nomMedecin(rdv) + " ne peut pas vous recevoir le " + formater(rdv.getDateHeure())
                        + (rdv.getMotifMedecin() != null ? " (" + rdv.getMotifMedecin() + ")" : "")
                        + ". Choisissez un autre créneau depuis l'application.",
                "/patient/rendez-vous", true);
        return rendezVousMapper.toResponse(rdv);
    }

    @Transactional
    public RendezVousResponse reporter(UUID id, Utilisateur medecin, DecisionRendezVousRequest decision) {
        RendezVous rdv = getPourMedecin(id, medecin);
        if (rdv.getStatut() != StatutRendezVous.EN_ATTENTE && rdv.getStatut() != StatutRendezVous.CONFIRME) {
            throw new ConflictException("Ce rendez-vous ne peut plus être reporté");
        }
        LocalDateTime nouvelle = decision.getNouvelleDateHeure();
        if (nouvelle == null || !nouvelle.isAfter(maintenant())) {
            throw new BadRequestException("Choisissez un nouveau créneau dans le futur");
        }
        disponibiliteService.verifierCreneau(rdv.getMedecin().getId(), nouvelle);
        if (rendezVousRepository.existsCreneauOccupeHors(rdv.getMedecin().getId(), nouvelle, rdv.getId())) {
            throw new BadRequestException("Ce créneau est déjà pris. Choisissez-en un autre.");
        }

        LocalDateTime ancienne = rdv.getDateHeure();
        if (rdv.getDateHeureInitiale() == null) {
            rdv.setDateHeureInitiale(ancienne);
        }
        rdv.setDateHeure(nouvelle);
        rdv.setStatut(StatutRendezVous.CONFIRME);
        rdv.setMotifMedecin(nettoyer(decision.getMotif()));
        rdv.setRappelEnvoye(false);
        rdv.setRappelProcheEnvoye(false);
        notificationService.notifier(rdv.getPatient(), "Rendez-vous reporté",
                "Le Dr " + nomMedecin(rdv) + " a déplacé votre rendez-vous du " + formater(ancienne)
                        + " au " + formater(nouvelle)
                        + (rdv.getMotifMedecin() != null ? " (" + rdv.getMotifMedecin() + ")" : "")
                        + ". Si cet horaire ne vous convient pas, vous pouvez l'annuler dans l'application.",
                "/patient/rendez-vous", true);
        return rendezVousMapper.toResponse(rdv);
    }

    /**
     * Changement de statut generique : le patient peut uniquement annuler ; le medecin peut annuler,
     * ou cloturer un rendez-vous passe (TERMINE / NO_SHOW). Accepter, refuser et reporter passent par
     * leurs actions dediees.
     */
    @Transactional
    public RendezVousResponse updateStatut(UUID id, StatutRendezVousRequest request, Utilisateur acteur) {
        RendezVous rdv = getOrThrow(id);
        StatutRendezVous nouveau = request.getStatut();
        boolean estPatient = rdv.getPatient().getId().equals(acteur.getId());
        boolean estMedecin = rdv.getMedecin().getId().equals(acteur.getId());
        boolean admin = acteur.getRole() == Role.ADMIN;
        if (!estPatient && !estMedecin && !admin) {
            throw new AccessDeniedException("Ce rendez-vous ne vous concerne pas");
        }
        if (StatutRendezVous.LIBERANT_LE_CRENEAU.contains(rdv.getStatut()) || rdv.getStatut() == StatutRendezVous.TERMINE) {
            throw new ConflictException("Ce rendez-vous est déjà clos");
        }

        if (nouveau == StatutRendezVous.ANNULE) {
            rdv.setStatut(StatutRendezVous.ANNULE);
            if (estPatient) {
                notificationService.notifier(rdv.getMedecin(), "Rendez-vous annulé par le patient",
                        rdv.getPatient().getPrenom() + " " + rdv.getPatient().getNom() + " a annulé son rendez-vous du "
                                + formater(rdv.getDateHeure()) + ". Le créneau est de nouveau libre.",
                        "/medecin/agenda", false);
            } else {
                notificationService.notifier(rdv.getPatient(), "Rendez-vous annulé",
                        "Votre rendez-vous du " + formater(rdv.getDateHeure()) + " avec le Dr " + nomMedecin(rdv)
                                + " a été annulé. Choisissez un autre créneau depuis l'application.",
                        "/patient/rendez-vous", true);
            }
        } else if (estPatient && !admin) {
            throw new AccessDeniedException("Le patient peut uniquement annuler son rendez-vous");
        } else if (STATUTS_MEDECIN_LIBRES.contains(nouveau)) {
            rdv.setStatut(nouveau);
        } else if (nouveau == StatutRendezVous.CONFIRME && rdv.getStatut() == StatutRendezVous.EN_ATTENTE) {
            return accepter(id, rdv.getMedecin());
        } else {
            throw new BadRequestException("Utilisez les actions accepter, refuser ou reporter");
        }
        return rendezVousMapper.toResponse(rdv);
    }

    @Transactional
    public void delete(UUID id) {
        rendezVousRepository.delete(getOrThrow(id));
    }

    // ------------------------------------------------------------------ Avis du patient

    @Transactional
    public AvisResponse donnerAvis(UUID rendezVousId, Utilisateur patient, AvisRequest request) {
        RendezVous rdv = getOrThrow(rendezVousId);
        if (!rdv.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("Vous ne pouvez noter que vos propres rendez-vous");
        }
        boolean effectue = rdv.getStatut() == StatutRendezVous.TERMINE
                || (rdv.getStatut() == StatutRendezVous.CONFIRME && rdv.getDateHeure().isBefore(maintenant()));
        if (!effectue) {
            throw new BadRequestException("Vous pourrez donner votre avis après le rendez-vous");
        }
        if (avisRepository.existsByRendezVousId(rendezVousId)) {
            throw new ConflictException("Vous avez déjà donné votre avis sur ce rendez-vous");
        }
        AvisMedecin avis = avisRepository.save(AvisMedecin.builder()
                .medecin(rdv.getMedecin())
                .patient(rdv.getPatient())
                .rendezVous(rdv)
                .note(request.getNote())
                .commentaire(nettoyer(request.getCommentaire()))
                .build());
        notificationService.notifier(rdv.getMedecin(), "Nouvel avis patient",
                "Un patient vous a attribué " + request.getNote() + "/5 après son rendez-vous du " + formater(rdv.getDateHeure()) + ".",
                "/medecin/profil", false);
        return toAvis(avis, false);
    }

    @Transactional(readOnly = true)
    public AvisMedecinResumeResponse avisMedecin(UUID medecinId, Utilisateur lecteur) {
        boolean admin = lecteur.getRole() == Role.ADMIN;
        List<AvisMedecin> avis = admin
                ? avisRepository.findByMedecinIdOrderByDateCreationDesc(medecinId)
                : avisRepository.findByMedecinIdAndMasqueFalseOrderByDateCreationDesc(medecinId);
        List<AvisMedecin> visibles = avis.stream().filter(a -> !a.isMasque()).toList();
        return AvisMedecinResumeResponse.builder()
                .medecinId(medecinId)
                .moyenne(visibles.isEmpty() ? null : visibles.stream().mapToInt(AvisMedecin::getNote).average().orElse(0))
                .nombre(visibles.size())
                .avis(avis.stream().map(a -> toAvis(a, admin)).toList())
                .build();
    }

    /** Moderation : l'administrateur masque (ou reaffiche) un avis abusif. */
    @Transactional
    public AvisResponse masquerAvis(UUID avisId, boolean masque, Utilisateur admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Réservé à l'administrateur");
        }
        AvisMedecin avis = avisRepository.findById(avisId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));
        avis.setMasque(masque);
        return toAvis(avis, true);
    }

    // ------------------------------------------------------------------ Outils

    private RendezVous getPourMedecin(UUID id, Utilisateur medecin) {
        RendezVous rdv = getOrThrow(id);
        if (!rdv.getMedecin().getId().equals(medecin.getId())) {
            throw new AccessDeniedException("Ce rendez-vous n'est pas dans votre agenda");
        }
        return rdv;
    }

    private RendezVous getOrThrow(UUID id) {
        return rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous non trouvé avec l'id : " + id));
    }

    private static AvisResponse toAvis(AvisMedecin a, boolean admin) {
        String nom = a.getPatient().getNom();
        return AvisResponse.builder()
                .id(a.getId())
                .medecinId(a.getMedecin().getId())
                .note(a.getNote())
                .commentaire(a.getCommentaire())
                .auteur(a.getPatient().getPrenom() + (nom != null && !nom.isEmpty() ? " " + nom.charAt(0) + "." : ""))
                .dateCreation(a.getDateCreation())
                .masque(admin ? a.isMasque() : null)
                .build();
    }

    static String formater(LocalDateTime dateHeure) {
        return dateHeure.format(FORMAT);
    }

    private static String libelleType(RendezVous rdv) {
        return rdv.getType() == TypeConsultation.TELECONSULTATION ? "en téléconsultation" : "au cabinet";
    }

    private static String nomMedecin(RendezVous rdv) {
        return rdv.getMedecin().getPrenom() + " " + rdv.getMedecin().getNom();
    }

    private static String nettoyer(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(Clock.system(ZoneId.of(fuseau)));
    }

    private String genererCodeConfirmation() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
