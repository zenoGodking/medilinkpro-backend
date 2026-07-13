package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.DemandeIntegrationRequest;
import com.medilinkpro.backend.dto.request.ReponseDemandeIntegrationRequest;
import com.medilinkpro.backend.dto.response.DemandeIntegrationResponse;
import com.medilinkpro.backend.service.DemandeIntegrationService;
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
 * Demandes d'integration d'un medecin dans un etablissement, dans les deux sens :
 * l'etablissement invite un medecin, ou le medecin demande a rejoindre un etablissement.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Integration Medecin-Etablissement", description = "Invitations et demandes d'integration d'un medecin dans un etablissement")
public class DemandeIntegrationController {

    private final DemandeIntegrationService demandeIntegrationService;

    @PostMapping("/api/etablissements/{etablissementId}/inviter-medecin/{medecinId}")
    @Operation(summary = "L'etablissement (Directeur/Admin) invite un medecin a le rejoindre")
    public ResponseEntity<DemandeIntegrationResponse> inviter(
            @PathVariable UUID etablissementId, @PathVariable UUID medecinId,
            @RequestBody(required = false) DemandeIntegrationRequest request) {
        DemandeIntegrationRequest body = request != null ? request : new DemandeIntegrationRequest();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(demandeIntegrationService.inviter(etablissementId, medecinId, body));
    }

    @PostMapping("/api/medecins/{medecinId}/demander-integration/{etablissementId}")
    @Operation(summary = "Le medecin demande a rejoindre un etablissement")
    public ResponseEntity<DemandeIntegrationResponse> demander(
            @PathVariable UUID medecinId, @PathVariable UUID etablissementId,
            @RequestBody(required = false) DemandeIntegrationRequest request) {
        DemandeIntegrationRequest body = request != null ? request : new DemandeIntegrationRequest();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(demandeIntegrationService.demander(medecinId, etablissementId, body));
    }

    @PatchMapping("/api/demandes-integration/{id}/repondre")
    @Operation(summary = "Accepter ou refuser une demande d'integration (medecin ou etablissement selon qui a initie)")
    public ResponseEntity<DemandeIntegrationResponse> repondre(
            @PathVariable UUID id, @RequestParam UUID actorId,
            @Valid @RequestBody ReponseDemandeIntegrationRequest request) {
        return ResponseEntity.ok(demandeIntegrationService.repondre(id, actorId, request));
    }

    @GetMapping("/api/medecins/{medecinId}/demandes-integration")
    @Operation(summary = "Historique des demandes d'integration d'un medecin")
    public ResponseEntity<List<DemandeIntegrationResponse>> listerParMedecin(@PathVariable UUID medecinId) {
        return ResponseEntity.ok(demandeIntegrationService.listerParMedecin(medecinId));
    }

    @GetMapping("/api/etablissements/{etablissementId}/demandes-integration")
    @Operation(summary = "Historique des demandes d'integration d'un etablissement")
    public ResponseEntity<List<DemandeIntegrationResponse>> listerParEtablissement(@PathVariable UUID etablissementId) {
        return ResponseEntity.ok(demandeIntegrationService.listerParEtablissement(etablissementId));
    }
}
