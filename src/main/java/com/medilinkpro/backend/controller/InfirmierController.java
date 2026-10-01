package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.DemandeIntegrationRequest;
import com.medilinkpro.backend.dto.response.DemandeIntegrationResponse;
import com.medilinkpro.backend.dto.response.InfirmierProfilResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.DemandeIntegrationService;
import com.medilinkpro.backend.service.EtablissementAccesService;
import com.medilinkpro.backend.service.InfirmierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Infirmieres", description = "Profil et photo des infirmieres, adhesion a un etablissement")
public class InfirmierController {

    private final InfirmierService infirmierService;
    private final DemandeIntegrationService demandeIntegrationService;
    private final EtablissementAccesService etablissementAccesService;

    @GetMapping("/api/infirmiers/moi")
    @Operation(summary = "Infirmiere : son propre profil")
    public ResponseEntity<InfirmierProfilResponse> monProfil(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(infirmierService.profil(utilisateur.getId(), utilisateur));
    }

    @PutMapping(value = "/api/infirmiers/moi/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Infirmiere : ajouter ou remplacer sa photo de profil (obligatoire pour repondre aux alertes)")
    public ResponseEntity<InfirmierProfilResponse> changerPhoto(
            @RequestPart("photo") MultipartFile photo, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(infirmierService.changerPhoto(utilisateur.getId(), photo));
    }

    @PostMapping("/api/infirmiers/moi/demander-integration/{etablissementId}")
    @Operation(summary = "Infirmiere : demander a rejoindre un etablissement (le directeur valide)")
    public ResponseEntity<DemandeIntegrationResponse> demanderIntegration(
            @PathVariable UUID etablissementId, @RequestBody(required = false) DemandeIntegrationRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        DemandeIntegrationRequest body = request != null ? request : new DemandeIntegrationRequest();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(demandeIntegrationService.demanderInfirmier(utilisateur.getId(), etablissementId, body));
    }

    @GetMapping("/api/infirmiers/moi/demandes-integration")
    @Operation(summary = "Infirmiere : historique de ses demandes d'adhesion")
    public ResponseEntity<List<DemandeIntegrationResponse>> mesDemandes(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(demandeIntegrationService.listerParInfirmier(utilisateur.getId()));
    }

    @GetMapping("/api/infirmiers/{id}/profil")
    @Operation(summary = "Profil d'une infirmiere (patients qu'elle a pris en charge, directeurs, admin)")
    public ResponseEntity<InfirmierProfilResponse> profil(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(infirmierService.profil(id, utilisateur));
    }

    @GetMapping("/api/infirmiers/{id}/photo")
    @Operation(summary = "Photo de profil d'une infirmiere (memes droits que le profil)")
    public ResponseEntity<byte[]> photo(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(infirmierService.photo(id, utilisateur));
    }

    @GetMapping("/api/etablissements/{etablissementId}/infirmiers")
    @Operation(summary = "Directeur/Admin : infirmieres rattachees a l'etablissement")
    public ResponseEntity<List<InfirmierProfilResponse>> parEtablissement(
            @PathVariable UUID etablissementId, @AuthenticationPrincipal Utilisateur utilisateur) {
        etablissementAccesService.verifierGestion(utilisateur, etablissementId);
        return ResponseEntity.ok(infirmierService.parEtablissement(etablissementId));
    }
}
