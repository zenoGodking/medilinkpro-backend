package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.MedecinUpdateRequest;
import com.medilinkpro.backend.dto.response.MedecinResponse;
import com.medilinkpro.backend.service.MedecinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medecins")
@RequiredArgsConstructor
@Tag(name = "Medecins", description = "Gestion des medecins/specialistes et recherche geolocalisee")
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
            summary = "Rechercher des specialistes geolocalises (F12)",
            description = "Filtre par specialite et/ou trie par proximite si latitude/longitude sont fournies"
    )
    public ResponseEntity<List<MedecinResponse>> rechercher(
            @Parameter(description = "Specialite recherchee, ex: cardiologie") @RequestParam(required = false) String specialite,
            @Parameter(description = "Latitude de l'utilisateur") @RequestParam(required = false) Double lat,
            @Parameter(description = "Longitude de l'utilisateur") @RequestParam(required = false) Double lng) {
        return ResponseEntity.ok(medecinService.rechercher(specialite, lat, lng));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre a jour un medecin")
    public ResponseEntity<MedecinResponse> update(@PathVariable UUID id, @Valid @RequestBody MedecinUpdateRequest request) {
        return ResponseEntity.ok(medecinService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un medecin")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medecinService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
