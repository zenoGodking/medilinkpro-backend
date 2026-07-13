package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.ValiderCompteRequest;
import com.medilinkpro.backend.dto.response.CompteEnAttenteResponse;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ConflictException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Administration des comptes utilisateurs : validation des inscriptions
 * professionnelles (Medecin, Secretaire, Directeur, Infirmier), et gestion
 * complete (liste, suspension, suppression) de tous les comptes.
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

    /** Liste tous les utilisateurs de la plateforme, tous statuts confondus (vue d'administration globale). */
    @Transactional(readOnly = true)
    public List<CompteEnAttenteResponse> listerTous(Role role) {
        List<Utilisateur> utilisateurs = role != null
                ? utilisateurRepository.findByRole(role)
                : utilisateurRepository.findAll();

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

    /**
     * Supprime definitivement un compte utilisateur, quel que soit son role.
     * Un administrateur ne peut pas se supprimer lui-meme (protection contre un
     * verrouillage accidentel de la plateforme). Si l'utilisateur possede des
     * donnees medicales liees (consultations, ordonnances, rendez-vous...), la
     * suppression est refusee : on privilegie la desactivation (toggleActif)
     * pour ne jamais perdre un historique medical.
     */
    @Transactional
    public void supprimer(UUID id, UUID adminCourantId) {
        if (id.equals(adminCourantId)) {
            throw new BadRequestException("Vous ne pouvez pas supprimer votre propre compte administrateur");
        }

        Utilisateur utilisateur = getOrThrow(id);

        try {
            utilisateurRepository.delete(utilisateur);
            utilisateurRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "Impossible de supprimer ce compte : il possede des donnees liees (consultations, "
                            + "rendez-vous, ordonnances, alertes...). Desactivez-le plutot pour preserver l'historique medical.");
        }
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
