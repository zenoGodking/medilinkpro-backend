package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.EtablissementMapper;
import com.medilinkpro.backend.dto.request.EtablissementRequest;
import com.medilinkpro.backend.dto.response.EtablissementResponse;
import com.medilinkpro.backend.entity.Directeur;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.repository.EtablissementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EtablissementService {

    private final com.medilinkpro.backend.repository.InfirmierRepository infirmierRepository;

    private final EtablissementRepository etablissementRepository;
    private final EtablissementMapper etablissementMapper;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<EtablissementResponse> findAll() {
        return etablissementRepository.findAll().stream()
                .map(etablissementMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EtablissementResponse findById(UUID id) {
        return etablissementMapper.toResponse(getOrThrow(id));
    }

    /**
     * Enregistre une visite publique de la fiche d'un etablissement (page vitrine,
     * accessible sans inscription) et retourne l'etablissement avec le compteur a jour.
     */
    @Transactional
    public EtablissementResponse enregistrerVisite(UUID id) {
        int lignesAffectees = etablissementRepository.incrementerVisites(id);
        if (lignesAffectees == 0) {
            throw new ResourceNotFoundException("Établissement non trouvé avec l'id : " + id);
        }
        return findById(id);
    }

    @Transactional
    public EtablissementResponse create(EtablissementRequest request, Utilisateur createur) {
        EtablissementSante etablissement = EtablissementSante.builder()
                .nom(request.getNom())
                .type(request.getType())
                .adresse(request.getAdresse())
                .ville(request.getVille())
                .quartier(request.getQuartier())
                .telephone(request.getTelephone())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .specialitesDisponibles(request.getSpecialitesDisponibles() != null
                        ? new ArrayList<>(request.getSpecialitesDisponibles())
                        : new ArrayList<>())
                // Un directeur qui cree un etablissement en devient le responsable.
                .directeur(createur instanceof Directeur d ? d : null)
                .build();

        return etablissementMapper.toResponse(etablissementRepository.save(etablissement));
    }

    @Transactional
    public EtablissementResponse update(UUID id, EtablissementRequest request) {
        EtablissementSante etablissement = getOrThrow(id);

        if (request.getNom() != null) etablissement.setNom(request.getNom());
        if (request.getType() != null) etablissement.setType(request.getType());
        if (request.getAdresse() != null) etablissement.setAdresse(request.getAdresse());
        if (request.getVille() != null) etablissement.setVille(request.getVille());
        if (request.getQuartier() != null) etablissement.setQuartier(request.getQuartier());
        if (request.getTelephone() != null) etablissement.setTelephone(request.getTelephone());
        if (request.getLatitude() != null && request.getLongitude() != null) {
            etablissement.setLatitude(request.getLatitude());
            etablissement.setLongitude(request.getLongitude());
        }
        if (request.getSpecialitesDisponibles() != null) {
            etablissement.setSpecialitesDisponibles(new ArrayList<>(request.getSpecialitesDisponibles()));
        }

        return etablissementMapper.toResponse(etablissementRepository.save(etablissement));
    }

    @Transactional
    public void delete(UUID id) {
        EtablissementSante etablissement = getOrThrow(id);
        etablissement.getPhotos().forEach(fileStorageService::deleteByUrl);
        infirmierRepository.findByEtablissementIdOrderByNomAsc(id).forEach(i -> i.setEtablissement(null));
        etablissementRepository.delete(etablissement);
    }

    /**
     * Upload et rattache une ou plusieurs photos a un etablissement. Les nouvelles
     * photos sont ajoutees a la suite des photos existantes (ordre = ordre du carousel).
     */
    @Transactional
    public EtablissementResponse ajouterPhotos(UUID id, List<MultipartFile> fichiers) {
        EtablissementSante etablissement = getOrThrow(id);

        if (fichiers == null || fichiers.isEmpty()) {
            throw new BadRequestException("Aucune photo envoyée");
        }

        List<String> nouvellesUrls = fileStorageService.storeImages(fichiers, "etablissements/" + id);
        etablissement.getPhotos().addAll(nouvellesUrls);

        return etablissementMapper.toResponse(etablissementRepository.save(etablissement));
    }

    /** Retire une photo de l'etablissement (et supprime le fichier physique associe). */
    @Transactional
    public EtablissementResponse supprimerPhoto(UUID id, String url) {
        EtablissementSante etablissement = getOrThrow(id);

        boolean removed = etablissement.getPhotos().removeIf(p -> p.equals(url));
        if (!removed) {
            throw new ResourceNotFoundException("Cette photo n'appartient pas à cet établissement");
        }
        fileStorageService.deleteByUrl(url);

        return etablissementMapper.toResponse(etablissementRepository.save(etablissement));
    }

    private EtablissementSante getOrThrow(UUID id) {
        return etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement non trouvé avec l'id : " + id));
    }
}
