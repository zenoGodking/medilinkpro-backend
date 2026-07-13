package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.AlerteRequest;
import com.medilinkpro.backend.dto.request.CompteRenduRequest;
import com.medilinkpro.backend.dto.request.NoterAlerteRequest;
import com.medilinkpro.backend.dto.response.AlerteResponse;
import com.medilinkpro.backend.dto.response.NoteMoyenneResponse;
import com.medilinkpro.backend.entity.AlerteSoinDomicile;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.enums.StatutAlerte;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ConflictException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.AlerteSoinDomicileRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Gere les alertes de soins a domicile envoyees par les patients (F-Alertes) :
 * creation + diffusion temps reel a toutes les infirmieres connectees sur
 * "/topic/alertes", puis attribution atomique a la premiere infirmiere qui
 * repond (voir AlerteSoinDomicileRepository.repondreSiDisponible). Le patient
 * est notifie en prive sur "/user/queue/alertes" des qu'une infirmiere repond.
 */
@Service
@RequiredArgsConstructor
public class AlerteService {

    private static final String TOPIC_ALERTES = "/topic/alertes";
    private static final String QUEUE_ALERTES = "/queue/alertes";

    private final AlerteSoinDomicileRepository alerteRepository;
    private final UtilisateurRepository utilisateurRepository;
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
        AlerteResponse response = toResponse(saved);

        // Diffusion temps reel a toutes les infirmieres actuellement connectees.
        messagingTemplate.convertAndSend(TOPIC_ALERTES, response);

        return response;
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

        int lignesAffectees = alerteRepository.repondreSiDisponible(alerteId, infirmier, LocalDateTime.now());
        if (lignesAffectees == 0) {
            throw new ConflictException("Cette alerte a deja ete prise en charge par une autre infirmiere (ou annulee)");
        }

        AlerteSoinDomicile alerte = alerteRepository.findById(alerteId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvee"));
        AlerteResponse response = toResponse(alerte);

        // Informe les autres infirmieres que l'alerte n'est plus disponible, et le
        // patient qu'une infirmiere a repondu ("alerte repondue").
        messagingTemplate.convertAndSend(TOPIC_ALERTES, response);
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
        messagingTemplate.convertAndSend(TOPIC_ALERTES, response);

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

        // L'alerte redevient visible pour toutes les infirmieres, et le patient est
        // informe que la recherche d'une nouvelle infirmiere reprend.
        messagingTemplate.convertAndSend(TOPIC_ALERTES, response);
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
        messagingTemplate.convertAndSend(TOPIC_ALERTES, response);

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

    @Transactional(readOnly = true)
    public List<AlerteResponse> listerActives() {
        return alerteRepository.findByStatutOrderByDateCreationDesc(StatutAlerte.EN_ATTENTE)
                .stream().map(this::toResponse).toList();
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
                .dateNotation(a.getDateNotation());

        if (a.getInfirmier() != null) {
            builder.infirmierId(a.getInfirmier().getId())
                    .infirmierNom(a.getInfirmier().getNom())
                    .infirmierPrenom(a.getInfirmier().getPrenom());
        }

        return builder.build();
    }
}
