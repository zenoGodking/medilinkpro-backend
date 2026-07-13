package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.MedecinMapper;
import com.medilinkpro.backend.dto.request.MedecinUpdateRequest;
import com.medilinkpro.backend.dto.response.MedecinResponse;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.EtablissementRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedecinService {

    private final MedecinRepository medecinRepository;
    private final EtablissementRepository etablissementRepository;
    private final MedecinMapper medecinMapper;

    @Transactional(readOnly = true)
    public List<MedecinResponse> findAll() {
        return medecinRepository.findAll().stream()
                .map(medecinMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MedecinResponse findById(UUID id) {
        return medecinMapper.toResponse(getMedecinOrThrow(id));
    }

    /**
     * Recherche de specialistes (Module 2 - F12), filtrable par specialite, ville et/ou quartier.
     */
    @Transactional(readOnly = true)
    public List<MedecinResponse> rechercher(String specialite, String ville, String quartier) {
        List<Medecin> medecins = medecinRepository.rechercher(
                blankToNull(specialite), blankToNull(ville), blankToNull(quartier));

        return medecins.stream().map(medecinMapper::toResponse).collect(Collectors.toList());
    }

    private String blankToNull(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur;
    }

    @Transactional
    public MedecinResponse update(UUID id, MedecinUpdateRequest request) {
        Medecin medecin = getMedecinOrThrow(id);

        if (request.getNom() != null) medecin.setNom(request.getNom());
        if (request.getPrenom() != null) medecin.setPrenom(request.getPrenom());
        if (request.getTelephone() != null) medecin.setTelephone(request.getTelephone());
        if (request.getSpecialite() != null) medecin.setSpecialite(request.getSpecialite());
        if (request.getNumeroOrdre() != null) medecin.setNumeroOrdre(request.getNumeroOrdre());
        if (request.getVille() != null) medecin.setVille(request.getVille());
        if (request.getQuartier() != null) medecin.setQuartier(request.getQuartier());
        if (request.getTarif() != null) medecin.setTarif(request.getTarif());
        if (request.getVerifie() != null) medecin.setVerifie(request.getVerifie());
        if (request.getEtablissementId() != null) {
            EtablissementSante etablissement = etablissementRepository.findById(request.getEtablissementId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Etablissement non trouve avec l'id : " + request.getEtablissementId()));
            medecin.setEtablissement(etablissement);
        }

        return medecinMapper.toResponse(medecinRepository.save(medecin));
    }

    @Transactional
    public void delete(UUID id) {
        Medecin medecin = getMedecinOrThrow(id);
        medecinRepository.delete(medecin);
    }

    private Medecin getMedecinOrThrow(UUID id) {
        return medecinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medecin non trouve avec l'id : " + id));
    }
}
