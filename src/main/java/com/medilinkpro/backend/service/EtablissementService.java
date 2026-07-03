package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.EtablissementMapper;
import com.medilinkpro.backend.dto.request.EtablissementRequest;
import com.medilinkpro.backend.dto.response.EtablissementResponse;
import com.medilinkpro.backend.entity.EtablissementSante;
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

    @Transactional
    public EtablissementResponse create(EtablissementRequest request) {
        EtablissementSante etablissement = EtablissementSante.builder()
                .nom(request.getNom())
                .type(request.getType())
                .adresse(request.getAdresse())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .telephone(request.getTelephone())
                .specialitesDisponibles(request.getSpecialitesDisponibles() != null
                        ? new ArrayList<>(request.getSpecialitesDisponibles())
                        : new ArrayList<>())
                .build();

        return etablissementMapper.toResponse(etablissementRepository.save(etablissement));
    }

    @Transactional
    public EtablissementResponse update(UUID id, EtablissementRequest request) {
        EtablissementSante etablissement = getOrThrow(id);

        if (request.getNom() != null) etablissement.setNom(request.getNom());
        if (request.getType() != null) etablissement.setType(request.getType());
        if (request.getAdresse() != null) etablissement.setAdresse(request.getAdresse());
        if (request.getLatitude() != null) etablissement.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) etablissement.setLongitude(request.getLongitude());
        if (request.getTelephone() != null) etablissement.setTelephone(request.getTelephone());
        if (request.getSpecialitesDisponibles() != null) {
            etablissement.setSpecialitesDisponibles(new ArrayList<>(request.getSpecialitesDisponibles()));
        }

        return etablissementMapper.toResponse(etablissementRepository.save(etablissement));
    }

    @Transactional
    public void delete(UUID id) {
        EtablissementSante etablissement = getOrThrow(id);
        etablissement.getPhotos().forEach(fileStorageService::deleteByUrl);
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
            throw new BadRequestException("Aucune photo envoyee");
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
            throw new ResourceNotFoundException("Cette photo n'appartient pas a cet etablissement");
        }
        fileStorageService.deleteByUrl(url);

        return etablissementMapper.toResponse(etablissementRepository.save(etablissement));
    }

    private EtablissementSante getOrThrow(UUID id) {
        return etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement non trouve avec l'id : " + id));
    }
}
