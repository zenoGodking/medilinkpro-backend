package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.DocumentMedicalRequest;
import com.medilinkpro.backend.dto.response.DocumentMedicalResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.DocumentMedicalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents-medicaux")
@RequiredArgsConstructor
@Tag(name = "Documents medicaux", description = "Antecedents, anciens carnets et documents scannes ou importes dans le carnet")
public class DocumentMedicalController {

    private final DocumentMedicalService documentService;

    @PostMapping(value = "/patients/{patientId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ajouter un document (1 a 20 photos/PDF) : le patient, ou un medecin autorise a ecrire dans son carnet")
    public ResponseEntity<DocumentMedicalResponse> ajouter(
            @PathVariable UUID patientId,
            @Valid @RequestPart("donnees") DocumentMedicalRequest donnees,
            @RequestPart("fichiers") List<MultipartFile> fichiers,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.ajouter(patientId, donnees, fichiers, utilisateur));
    }

    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Documents du carnet d'un patient (memes droits de lecture que le carnet)")
    public ResponseEntity<List<DocumentMedicalResponse>> lister(
            @PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(documentService.lister(patientId, utilisateur));
    }

    @GetMapping("/{documentId}/pages/{index}")
    @Operation(summary = "Telecharger une page (image ou PDF) d'un document")
    public ResponseEntity<byte[]> page(
            @PathVariable UUID documentId, @PathVariable int index, @AuthenticationPrincipal Utilisateur utilisateur) {
        DocumentMedicalService.Fichier fichier = documentService.lireFichier(documentId, index, utilisateur);
        MediaType type = fichier.typeMime() != null ? MediaType.parseMediaType(fichier.typeMime()) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(fichier.nom() != null ? fichier.nom() : "document", StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(fichier.contenu());
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Supprimer un document (le patient ou son auteur)")
    public ResponseEntity<Void> supprimer(@PathVariable UUID documentId, @AuthenticationPrincipal Utilisateur utilisateur) {
        documentService.supprimer(documentId, utilisateur);
        return ResponseEntity.noContent().build();
    }
}
