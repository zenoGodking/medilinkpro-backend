package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.EtablissementRequest;
import com.medilinkpro.backend.dto.response.EtablissementResponse;
import com.medilinkpro.backend.service.EtablissementService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/etablissements")
@RequiredArgsConstructor
@Tag(name = "Etablissements", description = "Hopitaux, cliniques et cabinets geolocalises (carte interactive - F11)")
public class EtablissementController {

    private final EtablissementService etablissementService;
    private final EtablissementAccesService etablissementAccesService;

    @GetMapping
    @Operation(summary = "Lister tous les etablissements de sante")
    public ResponseEntity<List<EtablissementResponse>> findAll() {
        return ResponseEntity.ok(etablissementService.findAll());
    }

    @GetMapping("/public")
    @Operation(summary = "Lister les etablissements pour la vitrine publique (page d'accueil, sans authentification)")
    public ResponseEntity<List<EtablissementResponse>> findAllPublic() {
        return ResponseEntity.ok(etablissementService.findAll());
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Fiche publique d'un etablissement (sans authentification)")
    public ResponseEntity<EtablissementResponse> findByIdPublic(@PathVariable UUID id) {
        return ResponseEntity.ok(etablissementService.findById(id));
    }

    @PostMapping("/public/{id}/visite")
    @Operation(summary = "Enregistrer une visite publique de la fiche (compteur visible par tous)")
    public ResponseEntity<EtablissementResponse> enregistrerVisite(@PathVariable UUID id) {
        return ResponseEntity.ok(etablissementService.enregistrerVisite(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un etablissement par son id")
    public ResponseEntity<EtablissementResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(etablissementService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Creer un etablissement de sante (un directeur en devient le responsable)")
    public ResponseEntity<EtablissementResponse> create(@Valid @RequestBody EtablissementRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.status(HttpStatus.CREATED).body(etablissementService.create(request, utilisateur));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre a jour un etablissement de sante (son directeur ou l'admin)")
    public ResponseEntity<EtablissementResponse> update(@PathVariable UUID id, @Valid @RequestBody EtablissementRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        etablissementAccesService.verifierGestion(utilisateur, id);
        return ResponseEntity.ok(etablissementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un etablissement de sante (son directeur ou l'admin)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        etablissementAccesService.verifierGestion(utilisateur, id);
        etablissementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/photos", consumes = "multipart/form-data")
    @Operation(summary = "Uploader une ou plusieurs photos pour un etablissement (carousel de la vitrine publique)")
    public ResponseEntity<EtablissementResponse> ajouterPhotos(
            @PathVariable UUID id,
            @RequestParam("fichiers") List<MultipartFile> fichiers, @AuthenticationPrincipal Utilisateur utilisateur) {
        etablissementAccesService.verifierGestion(utilisateur, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(etablissementService.ajouterPhotos(id, fichiers));
    }

    @DeleteMapping("/{id}/photos")
    @Operation(summary = "Supprimer une photo d'un etablissement")
    public ResponseEntity<EtablissementResponse> supprimerPhoto(
            @PathVariable UUID id,
            @RequestParam("url") String url, @AuthenticationPrincipal Utilisateur utilisateur) {
        etablissementAccesService.verifierGestion(utilisateur, id);
        return ResponseEntity.ok(etablissementService.supprimerPhoto(id, url));
    }
}
