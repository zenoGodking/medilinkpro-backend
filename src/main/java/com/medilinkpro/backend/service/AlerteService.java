package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.AlerteRequest;
import com.medilinkpro.backend.dto.request.CompteRenduRequest;
import com.medilinkpro.backend.dto.request.NoterAlerteRequest;
import com.medilinkpro.backend.dto.request.PositionRequest;
import com.medilinkpro.backend.dto.response.AlerteResponse;
import com.medilinkpro.backend.dto.response.NoteMoyenneResponse;
import com.medilinkpro.backend.dto.response.SuiviInfirmierResponse;
import com.medilinkpro.backend.entity.AlerteSoinDomicile;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutAlerte;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ConflictException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.AlerteSoinDomicileRepository;
import com.medilinkpro.backend.repository.InfirmierRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.util.Geo;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Gere les alertes de soins a domicile envoyees par les patients (F-Alertes) :
 * - creation puis notification des infirmieres disponibles les plus proches du patient
 *   (en prive sur "/user/queue/alertes"), elargie par vagues toutes les
 *   DELAI_ELARGISSEMENT si personne ne repond, jusqu'a une diffusion generale sur
 *   "/topic/alertes" en dernier recours (ou d'emblee si la position du patient est inconnue) ;
 * - attribution atomique a la premiere infirmiere qui repond
 *   (voir AlerteSoinDomicileRepository.repondreSiDisponible) ;
 * - suivi en temps reel : pendant l'intervention, chaque position envoyee par l'infirmiere
 *   est relayee au patient sur "/user/queue/suivi".
 */
@Service
@RequiredArgsConstructor
public class AlerteService {

    private static final String TOPIC_ALERTES = "/topic/alertes";
    private static final String QUEUE_ALERTES = "/queue/alertes";
    private static final String QUEUE_SUIVI = "/queue/suivi";

    /** Nombre d'infirmieres notifiees a chaque vague. */
    static final int TAILLE_VAGUE = 5;
    /** Au-dela, une infirmiere n'est pas consideree "a proximite". */
    static final double RAYON_MAX_KM = 20.0;
    /** Une position plus ancienne signifie que l'application de l'infirmiere est fermee. */
    static final long FRAICHEUR_POSITION_MINUTES = 15;
    /** Sans reponse apres ce delai, l'alerte est proposee aux infirmieres suivantes. */
    static final long DELAI_ELARGISSEMENT_SECONDES = 120;

    private final AlerteSoinDomicileRepository alerteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final InfirmierRepository infirmierRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public AlerteResponse creerAlerte(UUID patientId, AlerteRequest request) {
        Patient patient = (Patient) utilisateurRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient non trouve"));

        AlerteSoinDomicile alerte = AlerteSoinDomicile.builder()
                .patient(patient)
                .adresse(request.getAdresse())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .message(request.getMessage())
                .statut(StatutAlerte.EN_ATTENTE)
                .build();

        AlerteSoinDomicile saved = alerteRepository.save(alerte);
        diffuser(saved);
        return toResponse(saved);
    }

    /**
     * Propose l'alerte a la prochaine vague d'infirmieres disponibles les plus proches
     * (non encore notifiees, dans RAYON_MAX_KM). S'il n'y en a plus, ou si la position du
     * patient est inconnue, bascule en diffusion generale a toutes les infirmieres connectees.
     */
    void diffuser(AlerteSoinDomicile alerte) {
        alerte.setDateDerniereDiffusion(LocalDateTime.now());

        if (!Geo.coordonneesValides(alerte.getLatitude(), alerte.getLongitude())) {
            passerEnDiffusionGenerale(alerte);
            return;
        }

        LocalDateTime depuis = LocalDateTime.now().minusMinutes(FRAICHEUR_POSITION_MINUTES);
        List<InfirmierProche> vague = infirmierRepository.findDisponiblesLocalisees(depuis).stream()
                .filter(i -> !alerte.getInfirmiersNotifies().contains(i.getId()))
                .map(i -> new InfirmierProche(i, Geo.distanceKm(
                        alerte.getLatitude(), alerte.getLongitude(), i.getLatitude(), i.getLongitude())))
                .filter(p -> p.distanceKm() <= RAYON_MAX_KM)
                .sorted(Comparator.comparingDouble(InfirmierProche::distanceKm))
                .limit(TAILLE_VAGUE)
                .toList();

        if (vague.isEmpty()) {
            passerEnDiffusionGenerale(alerte);
            return;
        }

        vague.forEach(p -> alerte.getInfirmiersNotifies().add(p.infirmier().getId()));
        alerteRepository.save(alerte);

        AlerteResponse base = toResponse(alerte);
        apresCommit(() -> vague.forEach(p -> {
            AlerteResponse pourElle = toResponse(alerte);
            pourElle.setDistanceKm(arrondi(p.distanceKm()));
            messagingTemplate.convertAndSendToUser(p.infirmier().getEmail(), QUEUE_ALERTES, pourElle);
        }));
        apresCommit(() -> messagingTemplate.convertAndSendToUser(alerte.getPatient().getEmail(), QUEUE_ALERTES, base));
    }

    private void passerEnDiffusionGenerale(AlerteSoinDomicile alerte) {
        alerte.setDiffusionGenerale(true);
        alerteRepository.save(alerte);
        AlerteResponse response = toResponse(alerte);
        apresCommit(() -> {
            messagingTemplate.convertAndSend(TOPIC_ALERTES, response);
            messagingTemplate.convertAndSendToUser(alerte.getPatient().getEmail(), QUEUE_ALERTES, response);
        });
    }

    /** Elargit aux infirmieres suivantes les alertes restees sans reponse. */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void elargirAlertesSansReponse() {
        alerteRepository.findAElargir(LocalDateTime.now().minusSeconds(DELAI_ELARGISSEMENT_SECONDES))
                .forEach(this::diffuser);
    }

    /**
     * Informe les infirmieres concernees d'un changement d'etat : toutes si l'alerte est en
     * diffusion generale, sinon uniquement celles a qui elle a ete proposee.
     */
    private void notifierInfirmieres(AlerteSoinDomicile alerte, AlerteResponse response) {
        if (alerte.isDiffusionGenerale()) {
            messagingTemplate.convertAndSend(TOPIC_ALERTES, response);
            return;
        }
        List<UUID> ids = List.copyOf(alerte.getInfirmiersNotifies());
        infirmierRepository.findAllById(ids).forEach(i ->
                messagingTemplate.convertAndSendToUser(i.getEmail(), QUEUE_ALERTES, response));
    }

    /**
     * L'infirmiere envoie sa position GPS. Elle est memorisee (pour la notifier des alertes
     * proches) et, si elle a une intervention en cours, relayee en temps reel au patient.
     */
    @Transactional
    public void mettreAJourPosition(UUID infirmierId, PositionRequest position) {
        Infirmier infirmier = infirmierRepository.findById(infirmierId)
                .orElseThrow(() -> new AccessDeniedException("Seule une infirmiere peut partager sa position"));
        infirmier.setLatitude(position.getLatitude());
        infirmier.setLongitude(position.getLongitude());
        infirmier.setDatePosition(LocalDateTime.now());
        infirmierRepository.save(infirmier);

        alerteRepository.findFirstByInfirmierIdAndStatut(infirmierId, StatutAlerte.REPONDUE).ifPresent(alerte -> {
            SuiviInfirmierResponse suivi = toSuivi(alerte, infirmier);
            apresCommit(() -> messagingTemplate.convertAndSendToUser(alerte.getPatient().getEmail(), QUEUE_SUIVI, suivi));
        });
    }

    /** Derniere position connue de l'infirmiere en route, pour le patient proprietaire de l'alerte. */
    @Transactional(readOnly = true)
    public SuiviInfirmierResponse suivi(UUID alerteId, UUID patientId) {
        AlerteSoinDomicile alerte = alerteRepository.findById(alerteId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvee"));
        if (!alerte.getPatient().getId().equals(patientId)) {
            throw new AccessDeniedException("Cette alerte n'appartient pas a ce patient");
        }
        if (alerte.getStatut() != StatutAlerte.REPONDUE || alerte.getInfirmier() == null) {
            throw new BadRequestException("Le suivi n'est disponible que pendant l'intervention");
        }
        return toSuivi(alerte, alerte.getInfirmier());
    }

    private SuiviInfirmierResponse toSuivi(AlerteSoinDomicile alerte, Infirmier infirmier) {
        Double distance = Geo.coordonneesValides(alerte.getLatitude(), alerte.getLongitude())
                && Geo.coordonneesValides(infirmier.getLatitude(), infirmier.getLongitude())
                ? arrondi(Geo.distanceKm(alerte.getLatitude(), alerte.getLongitude(),
                        infirmier.getLatitude(), infirmier.getLongitude()))
                : null;
        return SuiviInfirmierResponse.builder()
                .alerteId(alerte.getId())
                .infirmierId(infirmier.getId())
                .latitude(infirmier.getLatitude())
                .longitude(infirmier.getLongitude())
                .datePosition(infirmier.getDatePosition())
                .distanceKm(distance)
                .build();
    }

    /** Envoie les messages WebSocket une fois la transaction validee (evite qu'une infirmiere
     * reponde a une alerte dont la notification n'est pas encore visible en base). */
    private static void apresCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private static double arrondi(double km) {
        return Math.round(km * 10) / 10.0;
    }

    private record InfirmierProche(Infirmier infirmier, double distanceKm) {
    }

    @Transactional
    public AlerteResponse repondre(UUID alerteId, UUID infirmierId) {
        Infirmier infirmier = (Infirmier) utilisateurRepository.findById(infirmierId)
                .orElseThrow(() -> new ResourceNotFoundException("Infirmier non trouve"));

        // Une infirmiere ne peut gerer qu'une seule intervention a la fois : elle doit
        // d'abord soumettre son compte-rendu avant de pouvoir repondre a une nouvelle alerte.
        boolean dejaOccupee = alerteRepository.existsByInfirmierIdAndStatut(infirmierId, StatutAlerte.REPONDUE);
        if (dejaOccupee) {
            throw new ConflictException(
                    "Vous avez deja une intervention en cours. Soumettez votre compte-rendu avant de repondre a une nouvelle alerte.");
        }

        AlerteSoinDomicile cible = alerteRepository.findById(alerteId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvee"));
        if (!cible.isDiffusionGenerale() && !cible.getInfirmiersNotifies().contains(infirmierId)) {
            throw new ConflictException("Cette alerte a ete proposee a des infirmieres plus proches du patient");
        }

        int lignesAffectees = alerteRepository.repondreSiDisponible(alerteId, infirmier, LocalDateTime.now());
        if (lignesAffectees == 0) {
            throw new ConflictException("Cette alerte a deja ete prise en charge par une autre infirmiere (ou annulee)");
        }

        AlerteSoinDomicile alerte = alerteRepository.findById(alerteId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvee"));
        AlerteResponse response = toResponse(alerte);

        // Informe les autres infirmieres que l'alerte n'est plus disponible, et le
        // patient qu'une infirmiere a repondu ("alerte repondue").
        notifierInfirmieres(alerte, response);
        messagingTemplate.convertAndSendToUser(alerte.getPatient().getEmail(), QUEUE_ALERTES, response);

        return response;
    }

    @Transactional
    public AlerteResponse annuler(UUID alerteId, UUID patientId) {
        AlerteSoinDomicile alerte = alerteRepository.findById(alerteId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvee"));

        if (!alerte.getPatient().getId().equals(patientId)) {
            throw new BadRequestException("Cette alerte n'appartient pas a ce patient");
        }
        if (alerte.getStatut() != StatutAlerte.EN_ATTENTE) {
            throw new BadRequestException("Cette alerte ne peut plus etre annulee");
        }

        alerte.setStatut(StatutAlerte.ANNULEE);
        AlerteResponse response = toResponse(alerteRepository.save(alerte));

        // Retire l'alerte de l'ecran des infirmieres.
        notifierInfirmieres(alerte, response);

        return response;
    }

    /**
     * Une infirmiere se retracte suite a un imprevu : elle n'est plus responsable
     * et l'alerte redevient EN_ATTENTE, a nouveau proposee a toutes les infirmieres
     * connectees (y compris elle-meme, si elle change d'avis).
     */
    @Transactional
    public AlerteResponse retracter(UUID alerteId, UUID infirmierId) {
        int lignesAffectees = alerteRepository.retracterSiResponsable(alerteId, infirmierId);
        if (lignesAffectees == 0) {
            throw new ConflictException("Vous n'etes plus responsable de cette alerte (deja terminee, annulee ou retractee)");
        }

        AlerteSoinDomicile alerte = alerteRepository.findById(alerteId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvee"));
        AlerteResponse response = toResponse(alerte);

        // L'alerte redevient visible pour les infirmieres deja notifiees (l'elargissement
        // reprend ensuite), et le patient est informe que la recherche reprend.
        notifierInfirmieres(alerte, response);
        messagingTemplate.convertAndSendToUser(alerte.getPatient().getEmail(), QUEUE_ALERTES, response);

        return response;
    }

    /**
     * L'infirmiere soumet son compte-rendu de fin d'intervention : l'alerte passe a
     * SERVICE_RENDU (le patient peut desormais la noter) et l'infirmiere est liberee,
     * elle peut de nouveau repondre a une nouvelle alerte EN_ATTENTE.
     */
    @Transactional
    public AlerteResponse soumettreCompteRendu(UUID alerteId, UUID infirmierId, CompteRenduRequest request) {
        int lignesAffectees = alerteRepository.soumettreCompteRenduSiResponsable(
                alerteId, infirmierId, request.getCompteRendu(), LocalDateTime.now());
        if (lignesAffectees == 0) {
            throw new ConflictException("Impossible de soumettre ce compte-rendu (intervention deja cloturee ou non responsable)");
        }

        AlerteSoinDomicile alerte = alerteRepository.findById(alerteId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvee"));
        AlerteResponse response = toResponse(alerte);

        // Le patient est informe que le compte-rendu est disponible et qu'il peut noter
        // l'intervention. Diffuse aussi aux infirmieres au cas ou l'ecran d'une autre
        // reflete l'etat de cette alerte.
        messagingTemplate.convertAndSendToUser(alerte.getPatient().getEmail(), QUEUE_ALERTES, response);
        notifierInfirmieres(alerte, response);

        return response;
    }

    /** Le patient note l'infirmiere a la fin du service rendu, ce qui cloture l'alerte. */
    @Transactional
    public AlerteResponse noter(UUID alerteId, UUID patientId, NoterAlerteRequest request) {
        int lignesAffectees = alerteRepository.noterSiEligible(
                alerteId, patientId, request.getNote(), request.getCommentaire(), LocalDateTime.now());
        if (lignesAffectees == 0) {
            throw new BadRequestException("Cette alerte ne peut pas etre notee dans son etat actuel");
        }

        AlerteSoinDomicile alerte = alerteRepository.findById(alerteId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvee"));
        AlerteResponse response = toResponse(alerte);

        // Informe l'infirmiere concernee (retire l'intervention de sa liste en cours).
        if (alerte.getInfirmier() != null) {
            messagingTemplate.convertAndSendToUser(alerte.getInfirmier().getEmail(), QUEUE_ALERTES, response);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public NoteMoyenneResponse noteMoyenne(UUID infirmierId) {
        Double moyenne = alerteRepository.moyenneNoteInfirmier(infirmierId);
        long nombreAvis = alerteRepository.countByInfirmierIdAndNoteIsNotNull(infirmierId);
        return NoteMoyenneResponse.builder().moyenne(moyenne).nombreAvis(nombreAvis).build();
    }

    @Transactional(readOnly = true)
    public List<AlerteResponse> listerInterventionsEnCours(UUID infirmierId) {
        return alerteRepository.findByInfirmierIdAndStatutOrderByDateReponseDesc(infirmierId, StatutAlerte.REPONDUE)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Alertes en attente visibles par l'utilisateur : pour une infirmiere, celles qui lui ont
     * ete proposees (ou en diffusion generale), avec sa distance au patient ; tout pour un admin.
     */
    @Transactional(readOnly = true)
    public List<AlerteResponse> listerActives(Utilisateur demandeur) {
        List<AlerteSoinDomicile> enAttente = alerteRepository.findByStatutOrderByDateCreationDesc(StatutAlerte.EN_ATTENTE);
        if (demandeur.getRole() != Role.INFIRMIER) {
            return enAttente.stream().map(this::toResponse).toList();
        }
        Infirmier infirmier = infirmierRepository.findById(demandeur.getId()).orElseThrow();
        return enAttente.stream()
                .filter(a -> a.isDiffusionGenerale() || a.getInfirmiersNotifies().contains(infirmier.getId()))
                .map(a -> {
                    AlerteResponse r = toResponse(a);
                    if (Geo.coordonneesValides(a.getLatitude(), a.getLongitude())
                            && Geo.coordonneesValides(infirmier.getLatitude(), infirmier.getLongitude())) {
                        r.setDistanceKm(arrondi(Geo.distanceKm(a.getLatitude(), a.getLongitude(),
                                infirmier.getLatitude(), infirmier.getLongitude())));
                    }
                    return r;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlerteResponse> listerMesAlertes(UUID patientId) {
        return alerteRepository.findByPatientIdOrderByDateCreationDesc(patientId)
                .stream().map(this::toResponse).toList();
    }

    private AlerteResponse toResponse(AlerteSoinDomicile a) {
        AlerteResponse.AlerteResponseBuilder builder = AlerteResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientNom(a.getPatient().getNom())
                .patientPrenom(a.getPatient().getPrenom())
                .patientTelephone(a.getPatient().getTelephone())
                .adresse(a.getAdresse())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .message(a.getMessage())
                .statut(a.getStatut())
                .dateCreation(a.getDateCreation())
                .dateReponse(a.getDateReponse())
                .compteRendu(a.getCompteRendu())
                .dateCompteRendu(a.getDateCompteRendu())
                .note(a.getNote())
                .commentaire(a.getCommentaire())
                .dateNotation(a.getDateNotation())
                .nombreInfirmiersNotifies(a.getInfirmiersNotifies().size())
                .diffusionGenerale(a.isDiffusionGenerale());

        if (a.getInfirmier() != null) {
            builder.infirmierId(a.getInfirmier().getId())
                    .infirmierNom(a.getInfirmier().getNom())
                    .infirmierPrenom(a.getInfirmier().getPrenom())
                    .infirmierTelephone(a.getInfirmier().getTelephone());
        }

        return builder.build();
    }
}
