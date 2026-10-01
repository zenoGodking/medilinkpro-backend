package com.medilinkpro.backend.config;

import com.medilinkpro.backend.entity.Admin;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cree au demarrage le super administrateur de base s'il n'existe pas encore
 * (l'inscription publique ne permet pas de creer un ADMIN). Idempotent : un compte
 * deja present n'est jamais modifie, son mot de passe change reste donc en vigueur.
 * Identifiants surchargeables par variables d'environnement (voir application.yml).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${medilinkpro.super-admin.nom}")
    private String nom;

    @Value("${medilinkpro.super-admin.prenom}")
    private String prenom;

    @Value("${medilinkpro.super-admin.email}")
    private String email;

    @Value("${medilinkpro.super-admin.mot-de-passe}")
    private String motDePasse;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (utilisateurRepository.existsByEmail(email)) {
            return;
        }
        utilisateurRepository.save(Admin.builder()
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .motDePasse(passwordEncoder.encode(motDePasse))
                .role(Role.ADMIN)
                .statutCompte(StatutCompte.APPROUVE)
                .actif(true)
                .build());
        log.info("Super administrateur de base créé : {}", email);
    }
}
