package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.RendezVousRequest;
import com.medilinkpro.backend.dto.request.StatutRendezVousRequest;
import com.medilinkpro.backend.dto.response.RendezVousResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.service.CarnetAccesService;
import com.medilinkpro.backend.service.RendezVousService;
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
 * Rendez-vous. Un rendez-vous confirme ou termine fait du patient un "ancien patient" du
 * medecin, ce qui donne a ce dernier le droit d'ecrire dans son carnet : seul le patient peut
 * donc reserver pour lui-meme (un medecin ne peut pas s'attribuer un patient).
 */
@RestController
@RequestMapping("/api/rendez-vous")
@RequiredArgsConstructor
@Tag(name = "Rendez-vous", description = "Prise de rendez-vous en ligne (Module 2 - F13 a F18)")
public class RendezVousController {

    private final RendezVousService rendezVousService;
    private final CarnetAccesService carnetAccesService;

    @GetMapping
    @Operation(summary = "Lister tous les rendez-vous (administrateur)")
    public ResponseEntity<List<RendezVousResponse>> findAll(@AuthenticationPrincipal Utilisateur utilisateur) {
        exigerAdmin(utilisateur);
        return ResponseEntity.ok(rendezVousService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un rendez-vous par son id (son patient, son medecin ou un administrateur)")
    public ResponseEntity<RendezVousResponse> findById(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        RendezVousResponse rdv = rendezVousService.findById(id);
        verifierParticipant(rdv, utilisateur);
        return ResponseEntity.ok(rdv);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Lister les rendez-vous d'un patient")
    public ResponseEntity<List<RendezVousResponse>> findByPatient(
            @PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLecture(utilisateur, patientId);
        return ResponseEntity.ok(rendezVousService.findByPatient(patientId));
    }

    @GetMapping("/medecin/{medecinId}")
    @Operation(summary = "Lister les rendez-vous d'un medecin (agenda : le medecin lui-meme ou un administrateur)")
    public ResponseEntity<List<RendezVousResponse>> findByMedecin(
            @PathVariable UUID medecinId, @AuthenticationPrincipal Utilisateur utilisateur) {
        if (utilisateur.getRole() != Role.ADMIN && !utilisateur.getId().equals(medecinId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que votre propre agenda");
        }
        return ResponseEntity.ok(rendezVousService.findByMedecin(medecinId));
    }

    @PostMapping
    @Operation(
            summary = "Prendre un rendez-vous (F13) - le patient pour lui-meme",
            description = "Verifie la disponibilite du creneau puis confirme le rendez-vous"
    )
    public ResponseEntity<RendezVousResponse> create(
            @Valid @RequestBody RendezVousRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        if (utilisateur.getRole() != Role.ADMIN && !utilisateur.getId().equals(request.getPatientId())) {
            throw new AccessDeniedException("Un rendez-vous ne peut etre pris que par le patient lui-meme");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(rendezVousService.create(request));
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Changer le statut d'un rendez-vous : le patient peut seulement annuler, le medecin gere son agenda")
    public ResponseEntity<RendezVousResponse> updateStatut(
            @PathVariable UUID id, @Valid @RequestBody StatutRendezVousRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        RendezVousResponse rdv = rendezVousService.findById(id);
        verifierParticipant(rdv, utilisateur);
        if (utilisateur.getId().equals(rdv.getPatientId()) && request.getStatut() != StatutRendezVous.ANNULE) {
            throw new AccessDeniedException("Le patient peut uniquement annuler son rendez-vous");
        }
        return ResponseEntity.ok(rendezVousService.updateStatut(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un rendez-vous (administrateur)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        exigerAdmin(utilisateur);
        rendezVousService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static void verifierParticipant(RendezVousResponse rdv, Utilisateur u) {
        boolean participant = u.getId().equals(rdv.getPatientId()) || u.getId().equals(rdv.getMedecinId());
        if (u.getRole() != Role.ADMIN && !participant) {
            throw new AccessDeniedException("Ce rendez-vous ne vous concerne pas");
        }
    }

    private static void exigerAdmin(Utilisateur u) {
        if (u.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Reserve a l'administrateur");
        }
    }
}
