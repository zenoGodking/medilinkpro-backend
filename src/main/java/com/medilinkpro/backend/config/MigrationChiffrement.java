package com.medilinkpro.backend.config;

import com.medilinkpro.backend.securite.ServiceChiffrement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Chiffre au demarrage les donnees de sante enregistrees avant l'activation du chiffrement :
 * champs texte sensibles encore en clair et fichiers prives sans en-tete de chiffrement.
 * Idempotent : les valeurs deja chiffrees (prefixe enc:v1:, en-tete MLPENC1) sont ignorees.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class MigrationChiffrement implements ApplicationRunner {

    /** Table -> colonnes chiffrees (doit correspondre aux @Convert(TexteChiffreConverter) des entites). */
    private static final Map<String, List<String>> COLONNES = Map.of(
            "patients", List.of("allergies", "antecedents", "conditions_urgence", "circonstances_deces"),
            "consultations", List.of("diagnostic", "compte_rendu"),
            "ordonnances", List.of("medicaments", "posologie"),
            "alertes_soin_domicile", List.of("compte_rendu"),
            "visites_prenatales", List.of("notes"),
            "documents_medicaux", List.of("description"));

    private final JdbcTemplate jdbcTemplate;
    private final ServiceChiffrement serviceChiffrement;
    private final FileStorageProperties fileStorageProperties;

    @Override
    public void run(ApplicationArguments args) {
        int champs = 0;
        for (var entree : COLONNES.entrySet()) {
            for (String colonne : entree.getValue()) {
                champs += chiffrerColonne(entree.getKey(), colonne);
            }
        }
        int fichiers = chiffrerFichiersPrives();
        if (champs > 0 || fichiers > 0) {
            log.info("Chiffrement des données existantes : {} champ(s) et {} fichier(s) chiffré(s)", champs, fichiers);
        }
    }

    private int chiffrerColonne(String table, String colonne) {
        List<Map<String, Object>> lignes = jdbcTemplate.queryForList(
                "SELECT id, " + colonne + " AS valeur FROM " + table
                        + " WHERE " + colonne + " IS NOT NULL AND " + colonne + " NOT LIKE ?",
                ServiceChiffrement.PREFIXE_TEXTE + "%");
        for (Map<String, Object> ligne : lignes) {
            jdbcTemplate.update("UPDATE " + table + " SET " + colonne + " = ? WHERE id = ?",
                    serviceChiffrement.chiffrerTexte((String) ligne.get("valeur")), ligne.get("id"));
        }
        return lignes.size();
    }

    private int chiffrerFichiersPrives() {
        Path racine = Paths.get(fileStorageProperties.getPrivateDir());
        if (!Files.isDirectory(racine)) {
            return 0;
        }
        int total = 0;
        try (Stream<Path> fichiers = Files.walk(racine)) {
            for (Path f : fichiers.filter(Files::isRegularFile).toList()) {
                byte[] contenu = Files.readAllBytes(f);
                if (!ServiceChiffrement.estFichierChiffre(contenu)) {
                    Files.write(f, serviceChiffrement.chiffrerFichier(contenu));
                    total++;
                }
            }
        } catch (IOException e) {
            log.error("Chiffrement des fichiers privés interrompu : {}", e.getMessage());
        }
        return total;
    }
}
