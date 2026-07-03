package com.medilinkpro.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de fumee : verifie que le contexte Spring se charge correctement
 * (toutes les beans, la configuration de securite, JPA, etc.).
 * Utilise application-test.yml avec une base H2 en memoire pour eviter
 * une dependance a PostgreSQL lors des tests unitaires rapides.
 */
@SpringBootTest
@ActiveProfiles("test")
class MedilinkproBackendApplicationTests {

    @Test
    void contextLoads() {
        // Si le contexte Spring demarre sans exception, le test passe.
    }
}
