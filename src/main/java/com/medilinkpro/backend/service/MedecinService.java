package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.MedecinMapper;
import com.medilinkpro.backend.dto.request.MedecinUpdateRequest;
import com.medilinkpro.backend.dto.response.MedecinResponse;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.EtablissementRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
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
    private final com.medilinkpro.backend.repository.AvisMedecinRepository avisRepository;

    @Transactional(readOnly = true)
    public List<MedecinResponse> findAll() {
        return avecAvis(medecinRepository.findAll());
    }

    @Transactional(readOnly = true)
    public MedecinResponse findById(UUID id) {
        return avecAvis(List.of(getMedecinOrThrow(id))).get(0);
    }

    /**
     * Recherche de specialistes (Module 2 - F12), filtrable par specialite, ville et/ou quartier.
     */
    @Transactional(readOnly = true)
    public List<MedecinResponse> rechercher(String specialite, String ville, String quartier) {
        List<Medecin> medecins = medecinRepository.rechercher(
                blankToNull(specialite), blankToNull(ville), blankToNull(quartier));

        return avecAvis(medecins);
    }

    /** Fiches medecin completees par la moyenne et le nombre de leurs avis (une seule requete). */
    private List<MedecinResponse> avecAvis(List<Medecin> medecins) {
        List<MedecinResponse> reponses = medecins.stream().map(medecinMapper::toResponse).collect(Collectors.toList());
        if (reponses.isEmpty()) {
            return reponses;
        }
        java.util.Map<UUID, Object[]> resumes = new java.util.HashMap<>();
        avisRepository.resumes(reponses.stream().map(MedecinResponse::getId).toList())
                .forEach(r -> resumes.put((UUID) r[0], r));
        reponses.forEach(m -> {
            Object[] r = resumes.get(m.getId());
            if (r != null) {
                m.setNoteMoyenne(((Number) r[1]).doubleValue());
                m.setNombreAvis(((Number) r[2]).longValue());
            }
        });
        return reponses;
    }

    private String blankToNull(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur;
    }

    /**
     * Mise a jour d'une fiche medecin :
     * - l'administrateur peut tout modifier (dont la verification et le rattachement) ;
     * - le medecin ne modifie que sa propre fiche et ses informations publiques. Son numero d'ordre
     *   (verifie a la validation du compte), le badge "verifie" et son etablissement (qui passe par une
     *   demande d'adhesion validee par le directeur) ne sont pas modifiables par lui.
     */
    @Transactional
    public MedecinResponse update(UUID id, MedecinUpdateRequest request, Utilisateur acteur) {
        Medecin medecin = getMedecinOrThrow(id);
        boolean admin = acteur.getRole() == Role.ADMIN;
        if (!admin && !acteur.getId().equals(id)) {
            throw new AccessDeniedException("Vous ne pouvez modifier que votre propre fiche");
        }
        if (!admin) {
            if (request.getVerifie() != null || request.getEtablissementId() != null) {
                throw new AccessDeniedException(
                        "La vérification et le rattachement à un établissement sont gérés par l'administration");
            }
            if (request.getNumeroOrdre() != null && !request.getNumeroOrdre().equals(medecin.getNumeroOrdre())) {
                throw new BadRequestException(
                        "Le numéro d'ordre a été vérifié à la validation de votre compte : contactez l'administrateur pour le modifier");
            }
        }

        if (request.getNom() != null) medecin.setNom(request.getNom());
        if (request.getPrenom() != null) medecin.setPrenom(request.getPrenom());
        if (request.getTelephone() != null) medecin.setTelephone(request.getTelephone());
        if (request.getSpecialite() != null) medecin.setSpecialite(request.getSpecialite());
        if (request.getVille() != null) medecin.setVille(request.getVille());
        if (request.getQuartier() != null) medecin.setQuartier(request.getQuartier());
        if (request.getTarif() != null) medecin.setTarif(request.getTarif());
        if (admin) {
            if (request.getNumeroOrdre() != null) medecin.setNumeroOrdre(request.getNumeroOrdre());
            if (request.getVerifie() != null) medecin.setVerifie(request.getVerifie());
            if (request.getEtablissementId() != null) {
                EtablissementSante etablissement = etablissementRepository.findById(request.getEtablissementId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Établissement non trouvé avec l'id : " + request.getEtablissementId()));
                medecin.setEtablissement(etablissement);
            }
        }

        return medecinMapper.toResponse(medecinRepository.save(medecin));
    }

    /** Suppression d'un compte medecin : reservee a l'administrateur. */
    @Transactional
    public void delete(UUID id, Utilisateur acteur) {
        if (acteur.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seul l'administrateur peut supprimer un compte médecin");
        }
        Medecin medecin = getMedecinOrThrow(id);
        medecinRepository.delete(medecin);
    }

    private Medecin getMedecinOrThrow(UUID id) {
        return medecinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin non trouvé avec l'id : " + id));
    }
}
