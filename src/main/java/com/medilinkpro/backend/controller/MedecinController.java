package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.MedecinUpdateRequest;
import com.medilinkpro.backend.dto.response.MedecinResponse;
import com.medilinkpro.backend.service.MedecinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.medilinkpro.backend.entity.Utilisateur;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medecins")
@RequiredArgsConstructor
@Tag(name = "Medecins", description = "Gestion des medecins/specialistes et recherche par ville/quartier")
public class MedecinController {

    private final MedecinService medecinService;

    @GetMapping
    @Operation(summary = "Lister tous les medecins")
    public ResponseEntity<List<MedecinResponse>> findAll() {
        return ResponseEntity.ok(medecinService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un medecin par son id")
    public ResponseEntity<MedecinResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(medecinService.findById(id));
    }

    @GetMapping("/recherche")
    @Operation(
            summary = "Rechercher des specialistes (F12)",
            description = "Filtre combinable par spécialité, ville et/ou quartier"
    )
    public ResponseEntity<List<MedecinResponse>> rechercher(
            @Parameter(description = "Specialite recherchee, ex: cardiologie") @RequestParam(required = false) String specialite,
            @Parameter(description = "Ville, ex: Yaounde") @RequestParam(required = false) String ville,
            @Parameter(description = "Quartier, ex: Bastos") @RequestParam(required = false) String quartier) {
        return ResponseEntity.ok(medecinService.rechercher(specialite, ville, quartier));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre a jour un medecin (le medecin lui-meme pour ses informations publiques, ou l'administrateur)")
    public ResponseEntity<MedecinResponse> update(@PathVariable UUID id, @Valid @RequestBody MedecinUpdateRequest request,
                                                  @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(medecinService.update(id, request, utilisateur));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un medecin (administrateur)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        medecinService.delete(id, utilisateur);
        return ResponseEntity.noContent().build();
    }
}
