package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.ReinitialisationMotDePasseRequest;
import com.medilinkpro.backend.entity.CodeReinitialisation;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.repository.CodeReinitialisationRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import com.medilinkpro.backend.securite.LimiteurTentatives;
import com.medilinkpro.backend.service.sms.NotificationSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Mot de passe oublie : un code a 6 chiffres, valable 15 minutes et a usage unique, est envoye par
 * SMS au numero du compte. La reponse est identique que le compte existe ou non (pas d'enumeration
 * des comptes). 5 essais au plus par code ; demandes limitees par email et par adresse IP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReinitialisationMotDePasseService {

    public static final String MESSAGE_ENVOI = "Si un compte existe avec cet email et un numéro de téléphone, "
            + "un code de réinitialisation vient d'être envoyé par SMS. Il est valable 15 minutes.";
    private static final Duration VALIDITE = Duration.ofMinutes(15);
    private static final Duration FENETRE_DEMANDES = Duration.ofHours(1);
    private static final int ESSAIS_MAX = 5;
    private static final SecureRandom ALEATOIRE = new SecureRandom();

    private final UtilisateurRepository utilisateurRepository;
    private final CodeReinitialisationRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationSmsService notificationSmsService;
    private final LimiteurTentatives limiteur;
    private final Environment environment;

    @Transactional
    public void demander(String email, String ip) {
        String cleEmail = "reinit-email:" + normaliser(email);
        String cleIp = "reinit-ip:" + ip;
        String message = "Trop de demandes de réinitialisation. Réessayez dans une heure.";
        limiteur.verifier(cleEmail, 3, FENETRE_DEMANDES, message);
        limiteur.verifier(cleIp, 10, FENETRE_DEMANDES, message);
        limiteur.enregistrer(cleEmail, FENETRE_DEMANDES);
        limiteur.enregistrer(cleIp, FENETRE_DEMANDES);

        utilisateurRepository.findByEmail(email.trim()).ifPresent(u -> {
            if (u.getTelephone() == null || u.getTelephone().isBlank() || !u.isActif()) {
                return;
            }
            codeRepository.findByUtilisateurIdAndUtiliseFalse(u.getId()).forEach(c -> c.setUtilise(true));
            String code = String.format("%06d", ALEATOIRE.nextInt(1_000_000));
            codeRepository.save(CodeReinitialisation.builder()
                    .utilisateur(u)
                    .codeHache(passwordEncoder.encode(code))
                    .expiration(LocalDateTime.now().plus(VALIDITE))
                    .build());
            String sms = "MediLinkPro : votre code de réinitialisation est " + code
                    + ". Il expire dans 15 minutes. Ne le communiquez à personne.";
            notificationSmsService.envoyer(u.getTelephone(), sms, "REINITIALISATION_MOT_DE_PASSE", null,
                    sms.replace(code, "******"));
            if (environment.acceptsProfiles(Profiles.of("dev"))) {
                log.info("[DEV] Code de réinitialisation pour {} : {}", u.getEmail(), code);
            }
        });
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void reinitialiser(ReinitialisationMotDePasseRequest request) {
        String invalide = "Code invalide ou expiré. Demandez un nouveau code.";
        Utilisateur u = utilisateurRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new BadRequestException(invalide));
        CodeReinitialisation code = codeRepository.findByUtilisateurIdAndUtiliseFalse(u.getId()).stream()
                .filter(c -> c.getExpiration().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(invalide));

        if (!passwordEncoder.matches(request.getCode(), code.getCodeHache())) {
            code.setTentatives(code.getTentatives() + 1);
            if (code.getTentatives() >= ESSAIS_MAX) {
                code.setUtilise(true);
            }
            throw new BadRequestException(code.isUtilise() ? invalide : "Code incorrect.");
        }

        code.setUtilise(true);
        u.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateurRepository.save(u);
        limiteur.reinitialiser("login:" + normaliser(u.getEmail()));
    }

    static String normaliser(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
