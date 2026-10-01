package com.medilinkpro.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Empeche le demarrage hors developpement avec une configuration dangereuse : secret JWT absent,
 * trop court ou egal a la cle de developpement (publique), mot de passe du super-admin faible ou
 * par defaut, ou origines CORS non definies. Le profil dev (mvn spring-boot:run) et les tests
 * ne sont pas concernes.
 */
@Slf4j
@Component
public class VerificationConfigurationProduction implements InitializingBean {

    static final String CLE_JWT_DEVELOPPEMENT =
            "NDgyZDcwYjQ3YTRmMzM5ZjU0MTk4ZTU0YjQ0YTQ4ODhlMzEzYjc4ZjY0YzQ4NzU5ZjA0ZTQ4ODg1ZjQ4ZTQ4";
    static final String CLE_CHIFFREMENT_DEVELOPPEMENT = "ZGV2LWNsZS1tZWRpbGlua3Byby0zMi1vY3RldHMhISE=";
    private static final List<String> MOTS_DE_PASSE_INTERDITS = List.of("Hope123", "admin", "password", "motdepasse");

    private final Environment environment;

    @Value("${medilinkpro.jwt.secret:}")
    private String jwtSecret;

    @Value("${medilinkpro.super-admin.email:}")
    private String adminEmail;

    @Value("${medilinkpro.super-admin.mot-de-passe:}")
    private String adminMotDePasse;

    @Value("${medilinkpro.cors.origines:}")
    private String origines;

    @Value("${medilinkpro.chiffrement.cle:}")
    private String cleChiffrement;

    public VerificationConfigurationProduction(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            log.warn("Profil de développement actif : secrets de développement autorisés. Ne pas utiliser en production.");
            return;
        }
        List<String> problemes = new ArrayList<>();
        if (jwtSecret.isBlank() || CLE_JWT_DEVELOPPEMENT.equals(jwtSecret)) {
            problemes.add("JWT_SECRET doit être défini et différent de la clé de développement (openssl rand -base64 48)");
        } else if (longueurOctets(jwtSecret) < 32) {
            problemes.add("JWT_SECRET doit faire au moins 256 bits (32 octets encodés en Base64)");
        }
        if (adminEmail.isBlank()) {
            problemes.add("SUPER_ADMIN_EMAIL doit être défini");
        }
        if (adminMotDePasse.length() < 12 || MOTS_DE_PASSE_INTERDITS.contains(adminMotDePasse)) {
            problemes.add("SUPER_ADMIN_PASSWORD doit faire au moins 12 caractères et ne pas être un mot de passe par défaut");
        }
        if (cleChiffrement.isBlank() || CLE_CHIFFREMENT_DEVELOPPEMENT.equals(cleChiffrement)) {
            problemes.add("CLE_CHIFFREMENT doit être définie et différente de la clé de développement (openssl rand -base64 32)");
        }
        if (origines.isBlank() || origines.contains("*")) {
            problemes.add("CORS_ORIGINES doit lister explicitement les URL du frontend (sans *)");
        }
        if (!problemes.isEmpty()) {
            throw new IllegalStateException("Configuration de production invalide :\n - " + String.join("\n - ", problemes));
        }
    }

    private static int longueurOctets(String base64) {
        try {
            return Base64.getDecoder().decode(base64).length;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }
}
