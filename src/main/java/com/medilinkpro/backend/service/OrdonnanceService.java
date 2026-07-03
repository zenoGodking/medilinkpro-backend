package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.OrdonnanceMapper;
import com.medilinkpro.backend.dto.request.OrdonnanceRequest;
import com.medilinkpro.backend.dto.response.OrdonnanceResponse;
import com.medilinkpro.backend.entity.Consultation;
import com.medilinkpro.backend.entity.Ordonnance;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.ConsultationRepository;
import com.medilinkpro.backend.repository.OrdonnanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de generation des ordonnances numeriques (Module 1 - F05, Module 3 - F22).
 * Une ordonnance est rattachee a une consultation existante (1 Consultation -> 0..1 Ordonnance).
 * Le code QR est simule par un identifiant unique ; en production, il encoderait
 * une URL de verification securisee consultable en pharmacie.
 */
@Service
@RequiredArgsConstructor
public class OrdonnanceService {

    private final OrdonnanceRepository ordonnanceRepository;
    private final ConsultationRepository consultationRepository;
    private final OrdonnanceMapper ordonnanceMapper;

    @Transactional(readOnly = true)
    public List<OrdonnanceResponse> findAll() {
        return ordonnanceRepository.findAll().stream()
                .map(ordonnanceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrdonnanceResponse findById(UUID id) {
        return ordonnanceMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OrdonnanceResponse> findByPatient(UUID patientId) {
        return ordonnanceRepository.findByPatientId(patientId).stream()
                .map(ordonnanceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrdonnanceResponse create(OrdonnanceRequest request) {
        Consultation consultation = consultationRepository.findById(request.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Consultation non trouvee avec l'id : " + request.getConsultationId()));

        if (ordonnanceRepository.findByConsultationId(consultation.getId()).isPresent()) {
            throw new BadRequestException("Une ordonnance existe deja pour cette consultation");
        }

        String codeQr = "QR-" + UUID.randomUUID();
        String signature = "SIGN-" + consultation.getMedecin().getId() + "-" + System.currentTimeMillis();

        Ordonnance ordonnance = Ordonnance.builder()
                .consultation(consultation)
                .medecin(consultation.getMedecin())
                .patient(consultation.getPatient())
                .medicaments(request.getMedicaments())
                .posologie(request.getPosologie())
                .signatureElectronique(signature)
                .codeQr(codeQr)
                .build();

        return ordonnanceMapper.toResponse(ordonnanceRepository.save(ordonnance));
    }

    private Ordonnance getOrThrow(UUID id) {
        return ordonnanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordonnance non trouvee avec l'id : " + id));
    }
}
