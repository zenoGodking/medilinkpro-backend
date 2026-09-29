package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.RechercheFacialeRequest;
import com.medilinkpro.backend.dto.response.CarnetUrgenceResponse;
import com.medilinkpro.backend.dto.response.RechercheFacialeResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.ReconnaissanceFacialeService;
import com.medilinkpro.backend.util.DescripteursJson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reconnaissance-faciale")
@RequiredArgsConstructor
@Tag(name = "Reconnaissance faciale", description = "Identification probable d'un patient accidente par sa photo (lecture seule)")
public class ReconnaissanceFacialeController {

    private final ReconnaissanceFacialeService reconnaissanceFacialeService;
    private final DescripteursJson descripteursJson;

    @PostMapping("/recherche")
    @Operation(summary = "Rechercher un patient par empreinte faciale",
            description = "Retourne au plus 3 correspondances probables avec score de confiance. "
                    + "Donnees essentielles pour tout utilisateur connecte, nom et date de naissance en plus pour le personnel de sante.")
    public ResponseEntity<RechercheFacialeResponse> rechercher(
            @Valid @RequestBody RechercheFacialeRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(reconnaissanceFacialeService.rechercher(request.getDescripteur(), utilisateur));
    }

    @GetMapping("/patients/{patientId}/carnet")
    @Operation(summary = "Carnet medical complet en lecture seule (personnel de sante valide uniquement)")
    public ResponseEntity<CarnetUrgenceResponse> carnet(
            @PathVariable UUID patientId,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(reconnaissanceFacialeService.carnetComplet(patientId, utilisateur));
    }

    @GetMapping("/moi/photo")
    @Operation(summary = "Photo faciale de reference du patient connecte")
    public ResponseEntity<Map<String, String>> maPhoto(@AuthenticationPrincipal Utilisateur utilisateur) {
        String photo = reconnaissanceFacialeService.photoPatientConnecte(utilisateur);
        return photo == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(Map.of("photo", photo));
    }

    @PutMapping(value = "/moi/photo", consumes = "multipart/form-data")
    @Operation(summary = "Enregistrer ou remplacer la photo faciale du patient connecte")
    public ResponseEntity<Void> enregistrerMaPhoto(
            @RequestPart("photo") MultipartFile photo,
            @RequestPart("descripteur") String descripteur,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        reconnaissanceFacialeService.enrolerPatientConnecte(utilisateur, photo, descripteursJson.lire(descripteur));
        return ResponseEntity.noContent().build();
    }
}
