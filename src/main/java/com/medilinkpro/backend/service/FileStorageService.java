package com.medilinkpro.backend.service;

import com.medilinkpro.backend.config.FileStorageProperties;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Stockage local des fichiers uploades (photos d'etablissements). En production,
 * le dossier configure (medilinkpro.upload.dir) doit etre monte sur un volume
 * persistant (voir docker-compose.yml).
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> EXTENSIONS_AUTORISEES = Set.of("jpg", "jpeg", "png", "webp");
    private static final long TAILLE_MAX_OCTETS = 5L * 1024 * 1024; // 5 Mo

    private final FileStorageProperties fileStorageProperties;

    /**
     * Enregistre une image sur disque, dans un sous-dossier donne, et retourne
     * l'URL publique (relative) a stocker en base de donnees.
     */
    public String storeImage(MultipartFile file, String subFolder) {
        Path stored = writeImage(file, Paths.get(fileStorageProperties.getDir(), subFolder));
        return fileStorageProperties.getUrlPrefix() + "/" + subFolder + "/" + stored.getFileName();
    }

    /**
     * Enregistre une image sensible (ex: photo faciale d'un patient) dans le dossier prive,
     * qui n'est pas servi statiquement. Retourne le chemin relatif a stocker en base ;
     * le fichier se relit uniquement via readPrivate.
     */
    public String storePrivateImage(MultipartFile file, String subFolder) {
        Path stored = writeImage(file, Paths.get(fileStorageProperties.getPrivateDir(), subFolder));
        return subFolder + "/" + stored.getFileName();
    }

    /** Relit un fichier du dossier prive a partir du chemin relatif retourne par storePrivateImage. */
    public byte[] readPrivate(String relativePath) {
        try {
            return Files.readAllBytes(resolvePrivate(relativePath));
        } catch (IOException e) {
            throw new ResourceNotFoundException("Fichier introuvable");
        }
    }

    public void deletePrivate(String relativePath) {
        if (relativePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolvePrivate(relativePath));
        } catch (IOException ignored) {
            // Suppression best-effort, comme deleteByUrl.
        }
    }

    private Path resolvePrivate(String relativePath) {
        Path root = Paths.get(fileStorageProperties.getPrivateDir()).toAbsolutePath().normalize();
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new BadRequestException("Chemin de fichier invalide");
        }
        return path;
    }

    private Path writeImage(MultipartFile file, Path dir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier envoye est vide");
        }
        if (file.getSize() > TAILLE_MAX_OCTETS) {
            throw new BadRequestException("L'image depasse la taille maximale autorisee (5 Mo)");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            throw new BadRequestException("Format d'image non supporte. Formats acceptes : jpg, jpeg, png, webp");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Le fichier envoye n'est pas une image valide");
        }

        try {
            Path targetDir = dir.toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            String filename = UUID.randomUUID() + "." + extension;
            Path targetPath = targetDir.resolve(filename).normalize();

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return targetPath;
        } catch (IOException e) {
            throw new BadRequestException("Erreur lors de l'enregistrement du fichier : " + e.getMessage());
        }
    }

    public List<String> storeImages(List<MultipartFile> files, String subFolder) {
        return files.stream().map(f -> storeImage(f, subFolder)).toList();
    }

    /** Supprime le fichier physique correspondant a une URL publique retournee par storeImage. */
    public void deleteByUrl(String url) {
        if (url == null || !url.startsWith(fileStorageProperties.getUrlPrefix())) {
            return;
        }
        String relative = url.substring(fileStorageProperties.getUrlPrefix().length());
        Path path = Paths.get(fileStorageProperties.getDir()).resolve("." + relative).normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Suppression best-effort : ne bloque pas la suppression en base si le fichier a deja disparu.
        }
    }

    private String extractExtension(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleaned.length() - 1) {
            throw new BadRequestException("Le fichier doit avoir une extension valide");
        }
        return cleaned.substring(dotIndex + 1).toLowerCase();
    }
}
