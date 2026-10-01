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
    private static final Set<String> EXTENSIONS_DOCUMENTS = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final long TAILLE_MAX_DOCUMENT_OCTETS = 10L * 1024 * 1024; // 10 Mo

    private final FileStorageProperties fileStorageProperties;
    private final com.medilinkpro.backend.securite.ServiceChiffrement serviceChiffrement;

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
        chiffrerSurPlace(stored);
        return subFolder + "/" + stored.getFileName();
    }

    /** Les fichiers prives (photos, documents medicaux) sont chiffres au repos (AES-256-GCM). */
    private void chiffrerSurPlace(Path chemin) {
        try {
            Files.write(chemin, serviceChiffrement.chiffrerFichier(Files.readAllBytes(chemin)));
        } catch (IOException e) {
            try { Files.deleteIfExists(chemin); } catch (IOException ignored) { /* best-effort */ }
            throw new BadRequestException("Erreur lors de l'enregistrement du fichier : " + e.getMessage());
        }
    }

    /**
     * Enregistre un document medical (photo scannee ou PDF) dans le dossier prive.
     * Retourne le chemin relatif a stocker en base ; le fichier se relit via readPrivate.
     */
    public String storePrivateDocument(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier envoyé est vide");
        }
        if (file.getSize() > TAILLE_MAX_DOCUMENT_OCTETS) {
            throw new BadRequestException("Le fichier dépasse la taille maximale autorisée (10 Mo)");
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (!EXTENSIONS_DOCUMENTS.contains(extension)) {
            throw new BadRequestException("Format non supporté. Formats acceptés : jpg, jpeg, png, webp, pdf");
        }
        String contentType = file.getContentType();
        boolean pdf = "pdf".equals(extension);
        if (contentType == null || (pdf ? !contentType.equals("application/pdf") : !contentType.startsWith("image/"))) {
            throw new BadRequestException("Le contenu du fichier ne correspond pas à son extension");
        }
        try {
            if (pdf && !commencePar(file, "%PDF")) {
                throw new BadRequestException("Le fichier n'est pas un PDF valide");
            }
            Path targetDir = Paths.get(fileStorageProperties.getPrivateDir(), subFolder).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(UUID.randomUUID() + "." + extension).normalize();
            Files.write(targetPath, serviceChiffrement.chiffrerFichier(file.getBytes()));
            return subFolder + "/" + targetPath.getFileName();
        } catch (IOException e) {
            throw new BadRequestException("Erreur lors de l'enregistrement du fichier : " + e.getMessage());
        }
    }

    private static boolean commencePar(MultipartFile file, String signature) throws IOException {
        byte[] attendu = signature.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        try (InputStream in = file.getInputStream()) {
            return java.util.Arrays.equals(in.readNBytes(attendu.length), attendu);
        }
    }

    /** Relit un fichier du dossier prive a partir du chemin relatif retourne par storePrivateImage. */
    public byte[] readPrivate(String relativePath) {
        try {
            return serviceChiffrement.dechiffrerFichier(Files.readAllBytes(resolvePrivate(relativePath)));
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
            throw new BadRequestException("Le fichier envoyé est vide");
        }
        if (file.getSize() > TAILLE_MAX_OCTETS) {
            throw new BadRequestException("L'image dépasse la taille maximale autorisée (5 Mo)");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            throw new BadRequestException("Format d'image non supporté. Formats acceptés : jpg, jpeg, png, webp");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Le fichier envoyé n'est pas une image valide");
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
