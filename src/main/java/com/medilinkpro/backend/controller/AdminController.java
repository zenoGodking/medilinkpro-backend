package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.ValiderCompteRequest;
import com.medilinkpro.backend.dto.response.CompteEnAttenteResponse;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Validation des comptes professionnels (Medecin, Infirmier, Directeur)")
public class AdminController {

    private final AdminService adminService;
    private final com.medilinkpro.backend.service.DirecteurService directeurService;

    @GetMapping("/comptes-en-attente")
    @Operation(summary = "Lister les comptes professionnels en attente de validation, filtrable par role")
    public ResponseEntity<List<CompteEnAttenteResponse>> listerComptesEnAttente(
            @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(adminService.listerComptesEnAttente(role));
    }

    @GetMapping("/utilisateurs")
    @Operation(summary = "Lister tous les utilisateurs de la plateforme, filtrable par role")
    public ResponseEntity<List<CompteEnAttenteResponse>> listerTous(
            @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(adminService.listerTous(role));
    }

    @DeleteMapping("/utilisateurs/{id}")
    @Operation(summary = "Supprimer definitivement un compte utilisateur, quel que soit son role")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id, @RequestParam UUID adminId) {
        adminService.supprimer(id, adminId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/comptes/{id}/valider")
    @Operation(summary = "Approuver ou refuser un compte professionnel en attente")
    public ResponseEntity<CompteEnAttenteResponse> valider(
            @PathVariable UUID id, @Valid @RequestBody ValiderCompteRequest request) {
        return ResponseEntity.ok(adminService.valider(id, request));
    }

    @PatchMapping("/comptes/{id}/remettre-en-attente")
    @Operation(summary = "Remettre un compte deja traite (approuve ou refuse) en attente de validation")
    public ResponseEntity<CompteEnAttenteResponse> remettreEnAttente(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.remettreEnAttente(id));
    }

    @PatchMapping("/comptes/{id}/toggle-actif")
    @Operation(summary = "Activer ou suspendre un compte utilisateur")
    public ResponseEntity<CompteEnAttenteResponse> toggleActif(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.toggleActif(id));
    }

    @PatchMapping("/etablissements/{id}/directeur")
    @Operation(summary = "Attribuer (ou retirer avec directeurId null) le directeur responsable d'un etablissement")
    public ResponseEntity<com.medilinkpro.backend.dto.response.EtablissementResponse> attribuerDirecteur(
            @PathVariable UUID id, @RequestBody com.medilinkpro.backend.dto.request.AttributionDirecteurRequest request) {
        return ResponseEntity.ok(directeurService.attribuerDirecteur(id, request.getDirecteurId()));
    }
}
