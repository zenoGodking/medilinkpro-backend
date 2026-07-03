package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.DossierMedicalMapper;
import com.medilinkpro.backend.dto.response.DossierMedicalResponse;
import com.medilinkpro.backend.entity.DossierMedical;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.DossierMedicalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de consultation du Dossier Medical Electronique (DME) - Module 1.
 * Le dossier est cree automatiquement a l'inscription du patient (voir AuthService) ;
 * ce service expose uniquement les operations de lecture/historique (F02, F06).
 */
@Service
@RequiredArgsConstructor
public class DossierMedicalService {

    private final DossierMedicalRepository dossierMedicalRepository;
    private final DossierMedicalMapper dossierMedicalMapper;

    @Transactional(readOnly = true)
    public List<DossierMedicalResponse> findAll() {
        return dossierMedicalRepository.findAll().stream()
                .map(dossierMedicalMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DossierMedicalResponse findById(UUID id) {
        return dossierMedicalMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public DossierMedicalResponse findByPatientId(UUID patientId) {
        DossierMedical dossier = dossierMedicalRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun dossier medical trouve pour le patient : " + patientId));
        return dossierMedicalMapper.toResponse(dossier);
    }

    private DossierMedical getOrThrow(UUID id) {
        return dossierMedicalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier medical non trouve avec l'id : " + id));
    }
}
