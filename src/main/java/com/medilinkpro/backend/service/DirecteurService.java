package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.EtablissementMapper;
import com.medilinkpro.backend.dto.response.EtablissementResponse;
import com.medilinkpro.backend.dto.response.PatientEtablissementResponse;
import com.medilinkpro.backend.entity.Directeur;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.EtablissementRepository;
import com.medilinkpro.backend.repository.RendezVousRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/** Espace directeur : ses etablissements et les patients qui y ont ete recus. */
@Service
@RequiredArgsConstructor
public class DirecteurService {

    private final EtablissementRepository etablissementRepository;
    private final RendezVousRepository rendezVousRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementMapper etablissementMapper;

    @Transactional(readOnly = true)
    public List<EtablissementResponse> mesEtablissements(Utilisateur directeur) {
        return etablissementRepository.findByDirecteurId(directeur.getId()).stream()
                .map(etablissementMapper::toResponse).toList();
    }

    /**
     * Patients ayant eu rendez-vous dans un de ses etablissements (rendez-vous rattache a
     * l'etablissement, ou pris avec un medecin de l'etablissement).
     */
    @Transactional(readOnly = true)
    public List<PatientEtablissementResponse> mesPatients(Utilisateur directeur) {
        Map<UUID, PatientEtablissementResponse> parPatient = new LinkedHashMap<>();
        for (RendezVous rdv : rendezVousRepository.findDansEtablissementsDuDirecteur(directeur.getId())) {
            Patient p = rdv.getPatient();
            EtablissementSante etablissement = rdv.getEtablissement() != null
                    ? rdv.getEtablissement() : rdv.getMedecin().getEtablissement();
            PatientEtablissementResponse ligne = parPatient.computeIfAbsent(p.getId(), id -> PatientEtablissementResponse.builder()
                    .id(p.getId()).nom(p.getNom()).prenom(p.getPrenom()).telephone(p.getTelephone())
                    .etablissements(new TreeSet<>()).build());
            ligne.setNombreRendezVous(ligne.getNombreRendezVous() + 1);
            if (ligne.getDernierRendezVous() == null || rdv.getDateHeure().isAfter(ligne.getDernierRendezVous())) {
                ligne.setDernierRendezVous(rdv.getDateHeure());
            }
            ligne.getEtablissements().add(etablissement.getNom());
        }
        return parPatient.values().stream()
                .sorted(Comparator.comparing(PatientEtablissementResponse::getDernierRendezVous).reversed())
                .toList();
    }

    /** Admin : attribue (ou retire, si directeurId est null) le directeur d'un etablissement. */
    @Transactional
    public EtablissementResponse attribuerDirecteur(UUID etablissementId, UUID directeurId) {
        EtablissementSante etablissement = etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement non trouvé"));
        Directeur directeur = null;
        if (directeurId != null) {
            if (!(utilisateurRepository.findById(directeurId).orElse(null) instanceof Directeur d)) {
                throw new BadRequestException("Cet utilisateur n'est pas un directeur");
            }
            directeur = d;
        }
        etablissement.setDirecteur(directeur);
        return etablissementMapper.toResponse(etablissement);
    }
}
