package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.RendezVousMapper;
import com.medilinkpro.backend.dto.request.RendezVousRequest;
import com.medilinkpro.backend.dto.request.StatutRendezVousRequest;
import com.medilinkpro.backend.dto.response.RendezVousResponse;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.EtablissementRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import com.medilinkpro.backend.repository.PatientRepository;
import com.medilinkpro.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des rendez-vous (Module 2 - Geolocalisation et Prise de Rendez-vous).
 * Implemente le scenario nominal F13/F14/F16/F18 decrit dans la modelisation :
 * verification de la disponibilite du creneau puis creation avec statut CONFIRME.
 */
@Service
@RequiredArgsConstructor
public class RendezVousService {

    private final RendezVousRepository rendezVousRepository;
    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final EtablissementRepository etablissementRepository;
    private final RendezVousMapper rendezVousMapper;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional(readOnly = true)
    public List<RendezVousResponse> findAll() {
        return rendezVousRepository.findAll().stream()
                .map(rendezVousMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RendezVousResponse findById(UUID id) {
        return rendezVousMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<RendezVousResponse> findByPatient(UUID patientId) {
        return rendezVousRepository.findByPatientId(patientId).stream()
                .map(rendezVousMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RendezVousResponse> findByMedecin(UUID medecinId) {
        return rendezVousRepository.findByMedecinId(medecinId).stream()
                .map(rendezVousMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RendezVousResponse create(RendezVousRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient non trouve avec l'id : " + request.getPatientId()));

        Medecin medecin = medecinRepository.findById(request.getMedecinId())
                .orElseThrow(() -> new ResourceNotFoundException("Medecin non trouve avec l'id : " + request.getMedecinId()));

        // Etape 4 du scenario : verifie la disponibilite du creneau avant insertion
        if (rendezVousRepository.existsCreneauOccupe(medecin.getId(), request.getDateHeure())) {
            throw new BadRequestException(
                    "Ce creneau n'est plus disponible pour ce medecin. Veuillez choisir un autre horaire.");
        }

        EtablissementSante etablissement = null;
        if (request.getEtablissementId() != null) {
            etablissement = etablissementRepository.findById(request.getEtablissementId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Etablissement non trouve avec l'id : " + request.getEtablissementId()));
        }

        RendezVous rdv = RendezVous.builder()
                .patient(patient)
                .medecin(medecin)
                .etablissement(etablissement)
                .dateHeure(request.getDateHeure())
                .type(request.getType())
                .statut(StatutRendezVous.CONFIRME)
                .rappelEnvoye(false)
                .codeConfirmation(genererCodeConfirmation())
                .build();

        RendezVous saved = rendezVousRepository.save(rdv);
        // F16 : notification automatique au patient et au medecin -> simule via NotificationService (log)
        return rendezVousMapper.toResponse(saved);
    }

    @Transactional
    public RendezVousResponse updateStatut(UUID id, StatutRendezVousRequest request) {
        RendezVous rdv = getOrThrow(id);
        rdv.setStatut(request.getStatut());
        return rendezVousMapper.toResponse(rendezVousRepository.save(rdv));
    }

    @Transactional
    public void delete(UUID id) {
        RendezVous rdv = getOrThrow(id);
        rendezVousRepository.delete(rdv);
    }

    private RendezVous getOrThrow(UUID id) {
        return rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous non trouve avec l'id : " + id));
    }

    private String genererCodeConfirmation() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
