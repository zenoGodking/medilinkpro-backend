package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.CampagneRequest;
import com.medilinkpro.backend.dto.response.CampagneResponse;
import com.medilinkpro.backend.entity.Campagne;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.CampagneRepository;
import com.medilinkpro.backend.repository.EtablissementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Campagnes publicitaires/sanitaires lancees par les etablissements (F-Campagnes) :
 * un Directeur peut annoncer une campagne (vaccination, depistage, promotion...)
 * visible publiquement sur la fiche de son etablissement, sans que le visiteur
 * ait besoin de s'inscrire.
 */
@Service
@RequiredArgsConstructor
public class CampagneService {

    private final CampagneRepository campagneRepository;
    private final EtablissementRepository etablissementRepository;

    @Transactional
    public CampagneResponse creer(UUID etablissementId, CampagneRequest request) {
        EtablissementSante etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement non trouve avec l'id : " + etablissementId));

        Campagne campagne = Campagne.builder()
                .etablissement(etablissement)
                .titre(request.getTitre())
                .description(request.getDescription())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .actif(true)
                .build();

        return toResponse(campagneRepository.save(campagne));
    }

    @Transactional(readOnly = true)
    public List<CampagneResponse> listerParEtablissement(UUID etablissementId) {
        return campagneRepository.findByEtablissementIdOrderByDateDebutDesc(etablissementId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CampagneResponse> listerActivesParEtablissement(UUID etablissementId) {
        return campagneRepository.findActivesByEtablissement(etablissementId, LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    /** Fil public de toutes les campagnes actives, tous etablissements confondus. */
    @Transactional(readOnly = true)
    public List<CampagneResponse> listerActives() {
        return campagneRepository.findActives(LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CampagneResponse desactiver(UUID id) {
        Campagne campagne = getOrThrow(id);
        campagne.setActif(false);
        return toResponse(campagneRepository.save(campagne));
    }

    @Transactional
    public void supprimer(UUID id) {
        campagneRepository.delete(getOrThrow(id));
    }

    /** Etablissement d'une campagne (controle d'acces du directeur). */
    @Transactional(readOnly = true)
    public UUID etablissementIdDe(UUID campagneId) {
        return getOrThrow(campagneId).getEtablissement().getId();
    }

    private Campagne getOrThrow(UUID id) {
        return campagneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne non trouvee avec l'id : " + id));
    }

    private CampagneResponse toResponse(Campagne c) {
        LocalDate aujourdhui = LocalDate.now();
        boolean estActive = c.isActif()
                && !c.getDateDebut().isAfter(aujourdhui)
                && (c.getDateFin() == null || !c.getDateFin().isBefore(aujourdhui));

        return CampagneResponse.builder()
                .id(c.getId())
                .etablissementId(c.getEtablissement().getId())
                .etablissementNom(c.getEtablissement().getNom())
                .titre(c.getTitre())
                .description(c.getDescription())
                .dateDebut(c.getDateDebut())
                .dateFin(c.getDateFin())
                .actif(c.isActif())
                .active(estActive)
                .dateCreation(c.getDateCreation())
                .build();
    }
}
