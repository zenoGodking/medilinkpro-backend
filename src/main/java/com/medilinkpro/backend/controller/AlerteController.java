package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.AlerteRequest;
import com.medilinkpro.backend.dto.request.CompteRenduRequest;
import com.medilinkpro.backend.dto.request.NoterAlerteRequest;
import com.medilinkpro.backend.dto.request.PositionRequest;
import com.medilinkpro.backend.dto.response.AlerteResponse;
import com.medilinkpro.backend.dto.response.NoteMoyenneResponse;
import com.medilinkpro.backend.dto.response.SuiviInfirmierResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.service.AlerteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Alertes de soins a domicile : un patient envoie une alerte proposee en temps reel
 * (WebSocket, voir WebSocketConfig) aux infirmieres les plus proches ;
 * la premiere a repondre devient responsable de l'intervention.
 *
 * Les identifiants patientId / infirmierId transmis dans l'URL doivent correspondre a
 * l'utilisateur connecte (voir verifierIdentite), sauf pour un administrateur.
 */
@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
@Tag(name = "Alertes soins a domicile", description = "Alertes patient -> infirmieres en temps reel")
public class AlerteController {

    private final AlerteService alerteService;

    @PostMapping("/patients/{patientId}")
    @Operation(summary = "Envoyer une alerte de soins a domicile aux infirmieres les plus proches")
    public ResponseEntity<AlerteResponse> creerAlerte(
            @PathVariable UUID patientId, @Valid @RequestBody AlerteRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        verifierIdentite(patientId, utilisateur);
        return ResponseEntity.status(HttpStatus.CREATED).body(alerteService.creerAlerte(patientId, request));
    }

    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Historique des alertes envoyees par un patient")
    public ResponseEntity<List<AlerteResponse>> mesAlertes(
            @PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur utilisateur) {
        verifierIdentite(patientId, utilisateur);
        return ResponseEntity.ok(alerteService.listerMesAlertes(patientId));
    }

    @GetMapping("/actives")
    @Operation(summary = "Alertes en attente proposees a l'infirmiere connectee (les plus proches d'elle), avec sa distance au patient")
    public ResponseEntity<List<AlerteResponse>> listerActives(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(alerteService.listerActives(utilisateur));
    }

    @PutMapping("/infirmiers/moi/position")
    @Operation(summary = "L'infirmiere connectee partage sa position (alternative REST au canal WebSocket /app/infirmiers/position)")
    public ResponseEntity<Void> partagerPosition(
            @Valid @RequestBody PositionRequest position, @AuthenticationPrincipal Utilisateur utilisateur) {
        alerteService.mettreAJourPosition(utilisateur.getId(), position);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/suivi")
    @Operation(summary = "Le patient consulte la derniere position de l'infirmiere en route (les suivantes arrivent sur /user/queue/suivi)")
    public ResponseEntity<SuiviInfirmierResponse> suivi(
            @PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(alerteService.suivi(id, utilisateur.getId()));
    }

    @PatchMapping("/{id}/repondre")
    @Operation(summary = "Une infirmiere repond 'present' a une alerte : elle en devient responsable")
    public ResponseEntity<AlerteResponse> repondre(
            @PathVariable UUID id, @RequestParam UUID infirmierId,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        verifierIdentite(infirmierId, utilisateur);
        return ResponseEntity.ok(alerteService.repondre(id, infirmierId));
    }

    @PatchMapping("/{id}/annuler")
    @Operation(summary = "Le patient annule sa propre alerte tant qu'aucune infirmiere n'a repondu")
    public ResponseEntity<AlerteResponse> annuler(
            @PathVariable UUID id, @RequestParam UUID patientId,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        verifierIdentite(patientId, utilisateur);
        return ResponseEntity.ok(alerteService.annuler(id, patientId));
    }

    @PatchMapping("/{id}/retracter")
    @Operation(summary = "Une infirmiere se retracte suite a un imprevu : l'alerte redevient disponible pour les autres")
    public ResponseEntity<AlerteResponse> retracter(
            @PathVariable UUID id, @RequestParam UUID infirmierId,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        verifierIdentite(infirmierId, utilisateur);
        return ResponseEntity.ok(alerteService.retracter(id, infirmierId));
    }

    @PatchMapping("/{id}/compte-rendu")
    @Operation(summary = "L'infirmiere soumet son compte-rendu de fin d'intervention : elle est alors liberee pour une nouvelle alerte")
    public ResponseEntity<AlerteResponse> soumettreCompteRendu(
            @PathVariable UUID id, @RequestParam UUID infirmierId, @Valid @RequestBody CompteRenduRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        verifierIdentite(infirmierId, utilisateur);
        return ResponseEntity.ok(alerteService.soumettreCompteRendu(id, infirmierId, request));
    }

    @PatchMapping("/{id}/noter")
    @Operation(summary = "Le patient note l'infirmiere a la fin du service rendu, ce qui cloture l'alerte")
    public ResponseEntity<AlerteResponse> noter(
            @PathVariable UUID id, @RequestParam UUID patientId, @Valid @RequestBody NoterAlerteRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        verifierIdentite(patientId, utilisateur);
        return ResponseEntity.ok(alerteService.noter(id, patientId, request));
    }

    @GetMapping("/infirmiers/{infirmierId}/en-cours")
    @Operation(summary = "Interventions actuellement assignees a une infirmiere (etat initial de sa liste)")
    public ResponseEntity<List<AlerteResponse>> interventionsEnCours(
            @PathVariable UUID infirmierId, @AuthenticationPrincipal Utilisateur utilisateur) {
        verifierIdentite(infirmierId, utilisateur);
        return ResponseEntity.ok(alerteService.listerInterventionsEnCours(infirmierId));
    }

    @GetMapping("/infirmiers/{infirmierId}/note-moyenne")
    @Operation(summary = "Note moyenne recue par une infirmiere sur ses interventions terminees")
    public ResponseEntity<NoteMoyenneResponse> noteMoyenne(@PathVariable UUID infirmierId) {
        return ResponseEntity.ok(alerteService.noteMoyenne(infirmierId));
    }

    /**
     * L'identifiant transmis (chemin ou parametre) doit etre celui de l'utilisateur connecte :
     * sans cette verification, un patient ou une infirmiere pourrait agir au nom d'un autre.
     * Un administrateur peut agir pour le compte de n'importe qui.
     */
    private static void verifierIdentite(UUID idDemande, Utilisateur utilisateur) {
        if (utilisateur.getRole() != Role.ADMIN && !utilisateur.getId().equals(idDemande)) {
            throw new AccessDeniedException("Vous ne pouvez agir que pour votre propre compte");
        }
    }
}
