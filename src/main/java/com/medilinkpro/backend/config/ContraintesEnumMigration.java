package com.medilinkpro.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate genere sur PostgreSQL des contraintes CHECK listant les valeurs des enums, mais avec
 * ddl-auto=update il ne les met jamais a jour quand une valeur est ajoutee (ex: role PHARMACIEN) :
 * les insertions echoueraient. On retire ces contraintes devenues obsoletes ; la valeur reste
 * validee par le type enum cote Java. Idempotent (IF EXISTS).
 * A terme, remplacer ddl-auto=update par des migrations versionnees (Flyway).
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class ContraintesEnumMigration implements ApplicationRunner {

    private static final String[][] CONTRAINTES_OBSOLETES = {
            {"acces_carnet", "acces_carnet_utilisateur_role_check"},
            {"mesures_sante", "mesures_sante_saisie_par_role_check"},
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String[] c : CONTRAINTES_OBSOLETES) {
            jdbcTemplate.execute("ALTER TABLE " + c[0] + " DROP CONSTRAINT IF EXISTS " + c[1]);
        }
    }
}
