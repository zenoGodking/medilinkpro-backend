package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.response.InfirmierProfilResponse;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutAlerte;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.AlerteSoinDomicileRepository;
import com.medilinkpro.backend.repository.InfirmierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Profil et photo des infirmieres. La photo est obligatoire (a l'inscription, ou ajoutee depuis
 * le profil pour les comptes existants) : le patient voit ainsi qui va venir chez lui des
 * qu'une infirmiere accepte sa demande de soins.
 * Profil/photo visibles par : l'infirmiere elle-meme, l'administrateur, les directeurs
 * (validation des demandes d'adhesion) et les patients qu'elle a pris en charge.
 */
@Service
@RequiredArgsConstructor
public class InfirmierService {

    public static final String DOSSIER_PHOTOS = "photos-infirmiers";
    private static final Set<StatutAlerte> INTERVENTIONS_REALISEES = Set.of(StatutAlerte.SERVICE_RENDU, StatutAlerte.TERMINEE);

    private final InfirmierRepository infirmierRepository;
    private final AlerteSoinDomicileRepository alerteRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public InfirmierProfilResponse profil(UUID infirmierId, Utilisateur lecteur) {
        Infirmier infirmier = getOrThrow(infirmierId);
        verifierVisibilite(infirmier, lecteur);
        return toProfil(infirmier, lecteur.getId().equals(infirmierId));
    }

    @Transactional(readOnly = true)
    public byte[] photo(UUID infirmierId, Utilisateur lecteur) {
        Infirmier infirmier = getOrThrow(infirmierId);
        verifierVisibilite(infirmier, lecteur);
        if (infirmier.getPhotoProfilChemin() == null) {
            throw new ResourceNotFoundException("Aucune photo pour cette infirmière");
        }
        return fileStorageService.readPrivate(infirmier.getPhotoProfilChemin());
    }

    /** Ajoute ou remplace la photo de profil de l'infirmiere connectee. */
    @Transactional
    public InfirmierProfilResponse changerPhoto(UUID infirmierId, MultipartFile photo) {
        Infirmier infirmier = getOrThrow(infirmierId);
        String ancienne = infirmier.getPhotoProfilChemin();
        infirmier.setPhotoProfilChemin(fileStorageService.storePrivateImage(photo, DOSSIER_PHOTOS));
        infirmierRepository.save(infirmier);
        fileStorageService.deletePrivate(ancienne);
        return toProfil(infirmier, true);
    }

    /** Infirmieres rattachees a un etablissement (la gestion est verifiee par l'appelant). */
    @Transactional(readOnly = true)
    public List<InfirmierProfilResponse> parEtablissement(UUID etablissementId) {
        return infirmierRepository.findByEtablissementIdOrderByNomAsc(etablissementId).stream()
                .map(i -> toProfil(i, false)).toList();
    }

    private void verifierVisibilite(Infirmier infirmier, Utilisateur u) {
        boolean autorise = u.getId().equals(infirmier.getId())
                || u.getRole() == Role.ADMIN
                || u.getRole() == Role.DIRECTEUR
                || (u.getRole() == Role.PATIENT && alerteRepository.existsByPatientIdAndInfirmierId(u.getId(), infirmier.getId()));
        if (!autorise) {
            throw new AccessDeniedException("Vous n'avez pas accès au profil de cette infirmière");
        }
    }

    private InfirmierProfilResponse toProfil(Infirmier i, boolean soiMeme) {
        return InfirmierProfilResponse.builder()
                .id(i.getId())
                .nom(i.getNom())
                .prenom(i.getPrenom())
                .telephone(i.getTelephone())
                .photoDisponible(i.getPhotoProfilChemin() != null)
                .etablissementId(i.getEtablissement() != null ? i.getEtablissement().getId() : null)
                .etablissementNom(i.getEtablissement() != null ? i.getEtablissement().getNom() : null)
                .noteMoyenne(alerteRepository.moyenneNoteInfirmier(i.getId()))
                .nombreAvis(alerteRepository.countByInfirmierIdAndNoteIsNotNull(i.getId()))
                .nombreInterventions(alerteRepository.countByInfirmierIdAndStatutIn(i.getId(), INTERVENTIONS_REALISEES))
                .membreDepuis(i.getDateInscription())
                .statutCompte(soiMeme ? i.getStatutCompte() : null)
                .build();
    }

    private Infirmier getOrThrow(UUID id) {
        return infirmierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Infirmier non trouvé avec l'id : " + id));
    }
}
