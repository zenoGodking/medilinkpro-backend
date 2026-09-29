package com.medilinkpro.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le role SECRETAIRE a ete retire de l'application. Les comptes secretaires deja en base
 * ne peuvent plus etre charges par Hibernate (valeur de role inconnue) : on les supprime
 * au demarrage, avec leur table dediee. Idempotent : sans effet une fois la base nettoyee.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SuppressionSecretairesMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS secretaires");
        int supprimes = jdbcTemplate.update("DELETE FROM utilisateurs WHERE role = 'SECRETAIRE'");
        if (supprimes > 0) {
            log.warn("Role SECRETAIRE retire : {} compte(s) secretaire supprime(s)", supprimes);
        }
    }
}
