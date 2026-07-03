package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.DossierMedicalMapper;
import com.medilinkpro.backend.dto.request.ResultatAnalyseRequest;
import com.medilinkpro.backend.dto.response.ResultatAnalyseResponse;
import com.medilinkpro.backend.entity.DossierMedical;
import com.medilinkpro.backend.entity.ResultatAnalyse;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.DossierMedicalRepository;
import com.medilinkpro.backend.repository.ResultatAnalyseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des resultats d'analyses (Module 1 - F04 : import des resultats
 * biologiques et d'imagerie depuis les laboratoires partenaires).
 */
@Service
@RequiredArgsConstructor
public class ResultatAnalyseService {

    private final ResultatAnalyseRepository resultatAnalyseRepository;
    private final DossierMedicalRepository dossierMedicalRepository;
    private final DossierMedicalMapper dossierMedicalMapper;

    @Transactional(readOnly = true)
    public List<ResultatAnalyseResponse> findByDossier(UUID dossierId) {
        return resultatAnalyseRepository.findByDossierMedicalId(dossierId).stream()
                .map(dossierMedicalMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResultatAnalyseResponse create(ResultatAnalyseRequest request) {
        DossierMedical dossier = dossierMedicalRepository.findById(request.getDossierMedicalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dossier medical non trouve avec l'id : " + request.getDossierMedicalId()));

        ResultatAnalyse resultat = ResultatAnalyse.builder()
                .dossierMedical(dossier)
                .type(request.getType())
                .dateResultat(request.getDateResultat())
                .fichierUrl(request.getFichierUrl())
                .statut(request.getStatut())
                .laboratoire(request.getLaboratoire())
                .build();

        return dossierMedicalMapper.toResponse(resultatAnalyseRepository.save(resultat));
    }

    @Transactional
    public void delete(UUID id) {
        ResultatAnalyse resultat = resultatAnalyseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resultat d'analyse non trouve avec l'id : " + id));
        resultatAnalyseRepository.delete(resultat);
    }
}
