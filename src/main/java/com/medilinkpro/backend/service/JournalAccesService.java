package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.response.AccesCarnetResponse;
import com.medilinkpro.backend.entity.AccesCarnet;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.TypeAccesCarnet;
import com.medilinkpro.backend.repository.AccesCarnetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Journal "qui a consulte mon carnet". Un meme acces (meme personne, meme patient, meme type)
 * n'est note qu'une fois par fenetre de FENETRE_DEDOUBLONNAGE_MINUTES : l'ouverture d'une page
 * qui declenche plusieurs lectures ne produit qu'une ligne.
 */
@Service
@RequiredArgsConstructor
public class JournalAccesService {

    static final long FENETRE_DEDOUBLONNAGE_MINUTES = 10;
    private static final int TAILLE_JOURNAL = 200;

    private final AccesCarnetRepository accesCarnetRepository;

    /**
     * Enregistre l'acces dans sa propre transaction : la trace est conservee meme si l'operation
     * appelante echoue ensuite, et l'appel fonctionne aussi depuis une transaction en lecture seule.
     * Les acces du patient a son propre carnet ne sont pas journalises.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(UUID patientId, Utilisateur utilisateur, TypeAccesCarnet type) {
        if (utilisateur.getId().equals(patientId)) {
            return;
        }
        LocalDateTime seuil = LocalDateTime.now().minusMinutes(FENETRE_DEDOUBLONNAGE_MINUTES);
        if (accesCarnetRepository.existsByPatientIdAndUtilisateurIdAndTypeAccesAndDateAccesAfter(
                patientId, utilisateur.getId(), type, seuil)) {
            return;
        }
        accesCarnetRepository.save(AccesCarnet.builder()
                .patientId(patientId)
                .utilisateurId(utilisateur.getId())
                .utilisateurRole(utilisateur.getRole())
                .utilisateurNom(utilisateur.getPrenom() + " " + utilisateur.getNom())
                .typeAcces(type)
                .build());
    }

    /**
     * Journal d'un patient. Les professionnels sont nommes (ils engagent leur responsabilite) ;
     * un autre patient ayant vu les donnees d'urgence via un scan reste anonyme.
     */
    @Transactional(readOnly = true)
    public List<AccesCarnetResponse> journal(UUID patientId) {
        return accesCarnetRepository.findByPatientIdOrderByDateAccesDesc(patientId, PageRequest.of(0, TAILLE_JOURNAL))
                .stream()
                .map(a -> AccesCarnetResponse.builder()
                        .dateAcces(a.getDateAcces())
                        .typeAcces(a.getTypeAcces())
                        .role(a.getUtilisateurRole())
                        .nom(a.getUtilisateurRole() == Role.PATIENT ? null : a.getUtilisateurNom())
                        .build())
                .toList();
    }
}
