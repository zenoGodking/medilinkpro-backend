package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.response.CarteUrgenceResponse;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.enums.TypeAccesCarnet;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Carte d'urgence : QR code (fond d'ecran de verrouillage, carte imprimee) menant aux
 * informations d'urgence du patient. Reservee aux utilisateurs connectes ; chaque scan
 * apparait dans le journal du patient.
 */
@Service
@RequiredArgsConstructor
public class CarteUrgenceService {

    private static final SecureRandom ALEATOIRE = new SecureRandom();
    private static final Set<Role> PERSONNEL_SANTE = Set.of(Role.MEDECIN, Role.INFIRMIER);

    private final PatientRepository patientRepository;
    private final ReconnaissanceFacialeService reconnaissanceFacialeService;
    private final JournalAccesService journalAccesService;

    /** Jeton de la carte du patient connecte (cree a la premiere demande). */
    @Transactional
    public String monJeton(Utilisateur utilisateur) {
        Patient patient = patientConnecte(utilisateur);
        if (patient.getJetonCarteUrgence() == null) {
            patient.setJetonCarteUrgence(nouveauJeton());
        }
        return patient.getJetonCarteUrgence();
    }

    /** Invalide l'ancienne carte (perdue, volee) : l'ancien QR code ne mene plus a rien. */
    @Transactional
    public String regenerer(Utilisateur utilisateur) {
        Patient patient = patientConnecte(utilisateur);
        patient.setJetonCarteUrgence(nouveauJeton());
        return patient.getJetonCarteUrgence();
    }

    @Transactional(readOnly = true)
    public CarteUrgenceResponse consulter(String jeton, Utilisateur lecteur) {
        Patient p = patientRepository.findByJetonCarteUrgence(jeton)
                .orElseThrow(() -> new ResourceNotFoundException("Carte d'urgence inconnue ou remplacée"));
        journalAccesService.enregistrer(p.getId(), lecteur, TypeAccesCarnet.CARTE_URGENCE);

        CarteUrgenceResponse.CarteUrgenceResponseBuilder carte = CarteUrgenceResponse.builder()
                .prenom(p.getPrenom())
                .photoReference(reconnaissanceFacialeService.photoDataUrl(p))
                .groupeSanguin(p.getGroupeSanguin())
                .allergies(p.getAllergies())
                .conditionsUrgence(p.getConditionsUrgence())
                .contactUrgenceNom(p.getContactUrgenceNom())
                .contactUrgenceTelephone(p.getContactUrgenceTelephone())
                .decede(p.isDecede());
        if (PERSONNEL_SANTE.contains(lecteur.getRole()) && lecteur.getStatutCompte() == StatutCompte.APPROUVE) {
            carte.patientId(p.getId()).nom(p.getNom()).dateNaissance(p.getDateNaissance());
        }
        return carte.build();
    }

    private Patient patientConnecte(Utilisateur u) {
        if (u.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Réservé au patient");
        }
        return patientRepository.findById(u.getId()).orElseThrow();
    }

    private static String nouveauJeton() {
        byte[] octets = new byte[24];
        ALEATOIRE.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }
}
