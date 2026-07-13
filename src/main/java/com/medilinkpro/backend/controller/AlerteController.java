package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.AlerteRequest;
import com.medilinkpro.backend.dto.request.CompteRenduRequest;
import com.medilinkpro.backend.dto.request.NoterAlerteRequest;
import com.medilinkpro.backend.dto.response.AlerteResponse;
import com.medilinkpro.backend.dto.response.NoteMoyenneResponse;
import com.medilinkpro.backend.service.AlerteService;
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
 * Alertes de soins a domicile : un patient envoie une alerte diffusee en temps
 * reel (WebSocket, voir WebSocketConfig) a toutes les infirmieres connectees ;
 * la premiere a repondre devient responsable de l'intervention.
 */
@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
@Tag(name = "Alertes soins a domicile", description = "Alertes patient -> infirmieres en temps reel")
public class AlerteController {

    private final AlerteService alerteService;

    @PostMapping("/patients/{patientId}")
    @Operation(summary = "Envoyer une alerte de soins a domicile a toutes les infirmieres connectees")
    public ResponseEntity<AlerteResponse> creerAlerte(
            @PathVariable UUID patientId, @Valid @RequestBody AlerteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alerteService.creerAlerte(patientId, request));
    }

    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Historique des alertes envoyees par un patient")
    public ResponseEntity<List<AlerteResponse>> mesAlertes(@PathVariable UUID patientId) {
        return ResponseEntity.ok(alerteService.listerMesAlertes(patientId));
    }

    @GetMapping("/actives")
    @Operation(summary = "Lister les alertes en attente de reponse (etat initial pour une infirmiere qui se connecte)")
    public ResponseEntity<List<AlerteResponse>> listerActives() {
        return ResponseEntity.ok(alerteService.listerActives());
    }

    @PatchMapping("/{id}/repondre")
    @Operation(summary = "Une infirmiere repond 'present' a une alerte : elle en devient responsable")
    public ResponseEntity<AlerteResponse> repondre(
            @PathVariable UUID id, @RequestParam UUID infirmierId) {
        return ResponseEntity.ok(alerteService.repondre(id, infirmierId));
    }

    @PatchMapping("/{id}/annuler")
    @Operation(summary = "Le patient annule sa propre alerte tant qu'aucune infirmiere n'a repondu")
    public ResponseEntity<AlerteResponse> annuler(
            @PathVariable UUID id, @RequestParam UUID patientId) {
        return ResponseEntity.ok(alerteService.annuler(id, patientId));
    }

    @PatchMapping("/{id}/retracter")
    @Operation(summary = "Une infirmiere se retracte suite a un imprevu : l'alerte redevient disponible pour les autres")
    public ResponseEntity<AlerteResponse> retracter(
            @PathVariable UUID id, @RequestParam UUID infirmierId) {
        return ResponseEntity.ok(alerteService.retracter(id, infirmierId));
    }

    @PatchMapping("/{id}/compte-rendu")
    @Operation(summary = "L'infirmiere soumet son compte-rendu de fin d'intervention : elle est alors liberee pour une nouvelle alerte")
    public ResponseEntity<AlerteResponse> soumettreCompteRendu(
            @PathVariable UUID id, @RequestParam UUID infirmierId, @Valid @RequestBody CompteRenduRequest request) {
        return ResponseEntity.ok(alerteService.soumettreCompteRendu(id, infirmierId, request));
    }

    @PatchMapping("/{id}/noter")
    @Operation(summary = "Le patient note l'infirmiere a la fin du service rendu, ce qui cloture l'alerte")
    public ResponseEntity<AlerteResponse> noter(
            @PathVariable UUID id, @RequestParam UUID patientId, @Valid @RequestBody NoterAlerteRequest request) {
        return ResponseEntity.ok(alerteService.noter(id, patientId, request));
    }

    @GetMapping("/infirmiers/{infirmierId}/en-cours")
    @Operation(summary = "Interventions actuellement assignees a une infirmiere (etat initial de sa liste)")
    public ResponseEntity<List<AlerteResponse>> interventionsEnCours(@PathVariable UUID infirmierId) {
        return ResponseEntity.ok(alerteService.listerInterventionsEnCours(infirmierId));
    }

    @GetMapping("/infirmiers/{infirmierId}/note-moyenne")
    @Operation(summary = "Note moyenne recue par une infirmiere sur ses interventions terminees")
    public ResponseEntity<NoteMoyenneResponse> noteMoyenne(@PathVariable UUID infirmierId) {
        return ResponseEntity.ok(alerteService.noteMoyenne(infirmierId));
    }
}
