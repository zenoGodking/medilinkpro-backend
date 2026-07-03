package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.ValiderCompteRequest;
import com.medilinkpro.backend.dto.response.CompteEnAttenteResponse;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Validation par un Admin des comptes professionnels (Medecin, Secretaire, Directeur)
 * crees via l'inscription publique (F-Auth) : approbation, refus, reouverture, suspension.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public List<CompteEnAttenteResponse> listerComptesEnAttente(Role role) {
        List<Utilisateur> utilisateurs = role != null
                ? utilisateurRepository.findByStatutCompteAndRole(StatutCompte.EN_ATTENTE, role)
                : utilisateurRepository.findByStatutCompte(StatutCompte.EN_ATTENTE);

        return utilisateurs.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CompteEnAttenteResponse valider(UUID id, ValiderCompteRequest request) {
        Utilisateur utilisateur = getOrThrow(id);

        if (Boolean.TRUE.equals(request.getApprouve())) {
            utilisateur.setStatutCompte(StatutCompte.APPROUVE);
            utilisateur.setMotifRejet(null);
        } else {
            if (request.getMotifRejet() == null || request.getMotifRejet().isBlank()) {
                throw new BadRequestException("Le motif de refus est obligatoire");
            }
            utilisateur.setStatutCompte(StatutCompte.REJETE);
            utilisateur.setMotifRejet(request.getMotifRejet());
        }

        return toResponse(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public CompteEnAttenteResponse remettreEnAttente(UUID id) {
        Utilisateur utilisateur = getOrThrow(id);
        utilisateur.setStatutCompte(StatutCompte.EN_ATTENTE);
        utilisateur.setMotifRejet(null);
        return toResponse(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public CompteEnAttenteResponse toggleActif(UUID id) {
        Utilisateur utilisateur = getOrThrow(id);
        utilisateur.setActif(!utilisateur.isActif());
        return toResponse(utilisateurRepository.save(utilisateur));
    }

    private Utilisateur getOrThrow(UUID id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouve avec l'id : " + id));
    }

    private CompteEnAttenteResponse toResponse(Utilisateur u) {
        CompteEnAttenteResponse.CompteEnAttenteResponseBuilder builder = CompteEnAttenteResponse.builder()
                .id(u.getId())
                .nom(u.getNom())
                .prenom(u.getPrenom())
                .email(u.getEmail())
                .telephone(u.getTelephone())
                .role(u.getRole())
                .statutCompte(u.getStatutCompte())
                .actif(u.isActif())
                .motifRejet(u.getMotifRejet())
                .dateInscription(u.getDateInscription());

        if (u instanceof Medecin medecin) {
            builder.specialite(medecin.getSpecialite()).numeroOrdre(medecin.getNumeroOrdre());
        }

        return builder.build();
    }
}
