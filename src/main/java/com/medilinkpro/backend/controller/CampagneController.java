package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.CampagneRequest;
import com.medilinkpro.backend.dto.response.CampagneResponse;
import com.medilinkpro.backend.service.CampagneService;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.EtablissementAccesService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Campagnes (vaccination, depistage, promotions...) lancees par les etablissements,
 * visibles publiquement sans que le visiteur ait besoin de s'inscrire.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Campagnes", description = "Campagnes publicitaires/sanitaires des etablissements, visibles publiquement")
public class CampagneController {

    private final CampagneService campagneService;
    private final EtablissementAccesService etablissementAccesService;

    @PostMapping("/api/etablissements/{etablissementId}/campagnes")
    @Operation(summary = "Lancer une nouvelle campagne pour un etablissement (Directeur, Admin)")
    public ResponseEntity<CampagneResponse> creer(
            @PathVariable UUID etablissementId, @Valid @RequestBody CampagneRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        etablissementAccesService.verifierGestion(utilisateur, etablissementId);
        return ResponseEntity.status(HttpStatus.CREATED).body(campagneService.creer(etablissementId, request));
    }

    @GetMapping("/api/etablissements/{etablissementId}/campagnes")
    @Operation(summary = "Historique complet des campagnes d'un etablissement (gestion interne)")
    public ResponseEntity<List<CampagneResponse>> listerParEtablissement(@PathVariable UUID etablissementId, @AuthenticationPrincipal Utilisateur utilisateur) {
        etablissementAccesService.verifierGestion(utilisateur, etablissementId);
        return ResponseEntity.ok(campagneService.listerParEtablissement(etablissementId));
    }

    @GetMapping("/api/etablissements/public/{etablissementId}/campagnes")
    @Operation(summary = "Campagnes actuellement actives d'un etablissement (fiche publique, sans authentification)")
    public ResponseEntity<List<CampagneResponse>> listerActivesParEtablissement(@PathVariable UUID etablissementId) {
        return ResponseEntity.ok(campagneService.listerActivesParEtablissement(etablissementId));
    }

    @GetMapping("/api/campagnes/actives")
    @Operation(summary = "Fil public de toutes les campagnes actives, tous etablissements confondus")
    public ResponseEntity<List<CampagneResponse>> listerActives() {
        return ResponseEntity.ok(campagneService.listerActives());
    }

    @PatchMapping("/api/campagnes/{id}/desactiver")
    @Operation(summary = "Arreter une campagne avant son terme (Directeur, Admin)")
    public ResponseEntity<CampagneResponse> desactiver(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        etablissementAccesService.verifierGestion(utilisateur, campagneService.etablissementIdDe(id));
        return ResponseEntity.ok(campagneService.desactiver(id));
    }

    @DeleteMapping("/api/campagnes/{id}")
    @Operation(summary = "Supprimer definitivement une campagne (Directeur, Admin)")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        etablissementAccesService.verifierGestion(utilisateur, campagneService.etablissementIdDe(id));
        campagneService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
