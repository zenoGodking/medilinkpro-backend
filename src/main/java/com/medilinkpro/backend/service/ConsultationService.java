package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.ConsultationMapper;
import com.medilinkpro.backend.dto.request.ConsultationRequest;
import com.medilinkpro.backend.dto.response.ConsultationResponse;
import com.medilinkpro.backend.entity.Consultation;
import com.medilinkpro.backend.entity.DossierMedical;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.ConsultationRepository;
import com.medilinkpro.backend.repository.DossierMedicalRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import com.medilinkpro.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des consultations (Module 1 - F03 : mise a jour des consultations).
 * Chaque consultation est automatiquement rattachee au DME du patient concerne.
 */
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final DossierMedicalRepository dossierMedicalRepository;
    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final ConsultationMapper consultationMapper;

    @Transactional(readOnly = true)
    public List<ConsultationResponse> findAll() {
        return consultationRepository.findAll().stream()
                .map(consultationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsultationResponse findById(UUID id) {
        return consultationMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponse> findByPatient(UUID patientId) {
        return consultationRepository.findByPatientId(patientId).stream()
                .map(consultationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponse> findByMedecin(UUID medecinId) {
        return consultationRepository.findByMedecinId(medecinId).stream()
                .map(consultationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConsultationResponse create(ConsultationRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient non trouve avec l'id : " + request.getPatientId()));

        Medecin medecin = medecinRepository.findById(request.getMedecinId())
                .orElseThrow(() -> new ResourceNotFoundException("Medecin non trouve avec l'id : " + request.getMedecinId()));

        DossierMedical dossier = dossierMedicalRepository.findByPatientId(patient.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun dossier medical trouve pour le patient : " + patient.getId()));

        Consultation consultation = Consultation.builder()
                .dossierMedical(dossier)
                .patient(patient)
                .medecin(medecin)
                .date(request.getDate())
                .motif(request.getMotif())
                .diagnostic(request.getDiagnostic())
                .compteRendu(request.getCompteRendu())
                .typeConsultation(request.getTypeConsultation())
                .build();

        return consultationMapper.toResponse(consultationRepository.save(consultation));
    }

    @Transactional
    public void delete(UUID id) {
        Consultation consultation = getOrThrow(id);
        consultationRepository.delete(consultation);
    }

    private Consultation getOrThrow(UUID id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation non trouvee avec l'id : " + id));
    }
}
