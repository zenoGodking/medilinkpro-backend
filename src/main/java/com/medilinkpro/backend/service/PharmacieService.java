package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.response.VerificationOrdonnanceResponse;
import com.medilinkpro.backend.entity.Ordonnance;
import com.medilinkpro.backend.entity.Pharmacien;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.TypeAccesCarnet;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ConflictException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.OrdonnanceRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verification et delivrance des ordonnances en pharmacie. Le patient presente le QR code de son
 * ordonnance ; le pharmacien la verifie (authenticite, validite, deja delivree ?) puis la delivre,
 * une seule fois. Chaque consultation apparait dans le journal du patient.
 */
@Service
@RequiredArgsConstructor
public class PharmacieService {

    /** Duree de validite d'une ordonnance a compter de son emission. */
    static final int VALIDITE_MOIS = 3;
    private static final SecureRandom ALEATOIRE = new SecureRandom();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final OrdonnanceRepository ordonnanceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JournalAccesService journalAccesService;

    /** Patient : jeton du QR code d'une de ses ordonnances (cree a la premiere demande). */
    @Transactional
    public Map<String, Object> qrPatient(UUID ordonnanceId, Utilisateur patient) {
        Ordonnance o = ordonnanceRepository.findById(ordonnanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordonnance introuvable"));
        if (!o.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("Cette ordonnance ne vous appartient pas");
        }
        if (o.getJetonVerification() == null) {
            byte[] octets = new byte[24];
            ALEATOIRE.nextBytes(octets);
            o.setJetonVerification(Base64.getUrlEncoder().withoutPadding().encodeToString(octets));
        }
        return Map.of(
                "jeton", o.getJetonVerification(),
                "delivree", o.getDateDelivrance() != null,
                "dateExpiration", expiration(o).toString());
    }

    @Transactional(readOnly = true)
    public VerificationOrdonnanceResponse verifier(String jeton, Utilisateur pharmacien) {
        Ordonnance o = parJeton(jeton);
        journalAccesService.enregistrer(o.getPatient().getId(), pharmacien, TypeAccesCarnet.PHARMACIE);
        return toResponse(o);
    }

    @Transactional
    public VerificationOrdonnanceResponse delivrer(String jeton, Utilisateur utilisateur) {
        Ordonnance o = parJeton(jeton);
        if (expiration(o).isBefore(LocalDate.now())) {
            throw new BadRequestException("Ordonnance expirée depuis le " + expiration(o) + " : elle ne peut plus être délivrée");
        }
        Pharmacien pharmacien = (Pharmacien) utilisateurRepository.findById(utilisateur.getId()).orElseThrow();
        String par = pharmacien.getPrenom() + " " + pharmacien.getNom()
                + (pharmacien.getNomPharmacie() != null ? " (" + pharmacien.getNomPharmacie() + ")" : "");
        int maj = ordonnanceRepository.delivrerSiDisponible(o.getId(), LocalDateTime.now(), pharmacien.getId(), par);
        if (maj == 0) {
            Ordonnance deja = ordonnanceRepository.findById(o.getId()).orElseThrow();
            throw new ConflictException("Ordonnance déjà délivrée le " + deja.getDateDelivrance().format(FORMAT)
                    + " par " + deja.getDelivreePar());
        }
        journalAccesService.enregistrer(o.getPatient().getId(), pharmacien, TypeAccesCarnet.PHARMACIE);
        return toResponse(ordonnanceRepository.findById(o.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<VerificationOrdonnanceResponse> mesDelivrances(Utilisateur pharmacien) {
        return ordonnanceRepository.findByPharmacienIdOrderByDateDelivranceDesc(pharmacien.getId()).stream()
                .map(this::toResponse).toList();
    }

    private Ordonnance parJeton(String jeton) {
        return ordonnanceRepository.findByJetonVerification(jeton)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ordonnance inconnue : ce QR code n'a pas été émis par MediLinkPro (possible falsification)"));
    }

    private static LocalDate expiration(Ordonnance o) {
        return o.getDateEmission().toLocalDate().plusMonths(VALIDITE_MOIS);
    }

    private VerificationOrdonnanceResponse toResponse(Ordonnance o) {
        LocalDate expiration = expiration(o);
        return VerificationOrdonnanceResponse.builder()
                .medicaments(o.getMedicaments())
                .posologie(o.getPosologie())
                .dateEmission(o.getDateEmission())
                .dateExpiration(expiration)
                .expiree(expiration.isBefore(LocalDate.now()))
                .delivree(o.getDateDelivrance() != null)
                .dateDelivrance(o.getDateDelivrance())
                .delivreePar(o.getDelivreePar())
                .patientNom(o.getPatient().getNom())
                .patientPrenom(o.getPatient().getPrenom())
                .patientDateNaissance(o.getPatient().getDateNaissance())
                .medecinNomComplet(o.getMedecin().getPrenom() + " " + o.getMedecin().getNom())
                .medecinSpecialite(o.getMedecin().getSpecialite())
                .medecinNumeroOrdre(o.getMedecin().getNumeroOrdre())
                .signatureElectronique(o.getSignatureElectronique())
                .build();
    }
}
