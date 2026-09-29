package com.medilinkpro.backend.service;

import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.EtablissementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gestion d'un etablissement (fiche, photos, campagnes, integration de medecins) :
 * l'administrateur, ou le directeur responsable de cet etablissement uniquement.
 */
@Service
@RequiredArgsConstructor
public class EtablissementAccesService {

    private final EtablissementRepository etablissementRepository;

    @Transactional(readOnly = true)
    public void verifierGestion(Utilisateur u, UUID etablissementId) {
        if (u.getRole() == Role.ADMIN) {
            return;
        }
        EtablissementSante etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement non trouve avec l'id : " + etablissementId));
        boolean estSonDirecteur = u.getRole() == Role.DIRECTEUR
                && etablissement.getDirecteur() != null
                && etablissement.getDirecteur().getId().equals(u.getId());
        if (!estSonDirecteur) {
            throw new AccessDeniedException("Vous ne gerez pas cet etablissement");
        }
    }
}
