package com.medilinkpro.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Parametres de stockage local des fichiers uploades (photos d'etablissements, etc.).
 * Voir application.yml -> medilinkpro.upload.
 */
@Component
@ConfigurationProperties(prefix = "medilinkpro.upload")
@Getter
@Setter
public class FileStorageProperties {

    /** Dossier disque ou sont ecrits les fichiers (monte en volume Docker en production). */
    private String dir = "uploads";

    /** Prefixe d'URL sous lequel les fichiers sont servis (voir WebMvcConfig). */
    private String urlPrefix = "/uploads";
}
