package com.medilinkpro.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Expose le dossier de stockage local des uploads (photos d'etablissements) comme
 * ressources statiques accessibles publiquement en lecture, ex: /uploads/etablissements/xxx.jpg
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = fileStorageProperties.getDir();
        String absolutePath = new File(uploadDir).getAbsolutePath();

        registry.addResourceHandler(fileStorageProperties.getUrlPrefix() + "/**")
                .addResourceLocations("file:" + absolutePath + File.separator);
    }
}
