package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.response.MotDePasseTemporaireResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.CodeReinitialisationRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.securite.LimiteurTentatives;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Mot de passe perdu : l'administrateur genere un nouveau mot de passe pour l'utilisateur.
 * Il lui est affiche une seule fois (seul son hache est stocke) pour qu'il le transmette.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MotDePasseService {

    // Sans caracteres ambigus a l'oral ou a l'ecrit (0/O, 1/I/L)
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATOIRE = new SecureRandom();

    private final UtilisateurRepository utilisateurRepository;
    private final CodeReinitialisationRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final LimiteurTentatives limiteur;

    @Transactional
    public MotDePasseTemporaireResponse genererParAdmin(UUID utilisateurId, Utilisateur admin) {
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Réservé à l'administrateur");
        }
        if (admin.getId().equals(utilisateurId)) {
            throw new BadRequestException("Vous ne pouvez pas régénérer votre propre mot de passe ici");
        }
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        String nouveau = genererMotDePasse();
        u.setMotDePasse(passwordEncoder.encode(nouveau));
        utilisateurRepository.save(u);
        // L'ancien blocage apres echecs et les codes SMS en cours ne doivent plus gener l'utilisateur
        codeRepository.findByUtilisateurIdAndUtiliseFalse(u.getId()).forEach(c -> c.setUtilise(true));
        limiteur.reinitialiser("login:" + ReinitialisationMotDePasseService.normaliser(u.getEmail()));
        log.info("Nouveau mot de passe genere pour l'utilisateur {} par l'administrateur {}", u.getId(), admin.getId());

        return MotDePasseTemporaireResponse.builder().nouveauMotDePasse(nouveau).build();
    }

    static String genererMotDePasse() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            sb.append(ALPHABET.charAt(ALEATOIRE.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
