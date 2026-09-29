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

    /**
     * Dossier disque des fichiers sensibles (photos faciales des patients). Il n'est PAS
     * expose par WebMvcConfig : ces fichiers ne sont servis que par des endpoints authentifies.
     */
    private String privateDir = "private-uploads";
}
