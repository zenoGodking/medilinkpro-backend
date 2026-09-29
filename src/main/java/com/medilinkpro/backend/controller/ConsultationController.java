package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.ConsultationRequest;
import com.medilinkpro.backend.dto.response.ConsultationResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.service.CarnetAccesService;
import com.medilinkpro.backend.service.ConsultationService;
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

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Tag(name = "Consultations", description = "Comptes rendus medicaux, diagnostics (Module 1 - F03)")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final CarnetAccesService carnetAccesService;

    @GetMapping
    @Operation(summary = "Lister toutes les consultations (medecins et administrateurs)")
    public ResponseEntity<List<ConsultationResponse>> findAll(@AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLectureGlobale(utilisateur);
        return ResponseEntity.ok(consultationService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une consultation par son id")
    public ResponseEntity<ConsultationResponse> findById(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        ConsultationResponse consultation = consultationService.findById(id);
        carnetAccesService.verifierLecture(utilisateur, consultation.getPatientId());
        return ResponseEntity.ok(consultation);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Lister les consultations d'un patient")
    public ResponseEntity<List<ConsultationResponse>> findByPatient(
            @PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLecture(utilisateur, patientId);
        return ResponseEntity.ok(consultationService.findByPatient(patientId));
    }

    @GetMapping("/medecin/{medecinId}")
    @Operation(summary = "Lister les consultations realisees par un medecin (le medecin lui-meme ou un administrateur)")
    public ResponseEntity<List<ConsultationResponse>> findByMedecin(
            @PathVariable UUID medecinId, @AuthenticationPrincipal Utilisateur utilisateur) {
        if (utilisateur.getRole() != Role.ADMIN && !utilisateur.getId().equals(medecinId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres consultations");
        }
        return ResponseEntity.ok(consultationService.findByMedecin(medecinId));
    }

    @PostMapping
    @Operation(summary = "Enregistrer une consultation (medecin autorise par le patient, ou ancien patient)")
    public ResponseEntity<ConsultationResponse> create(
            @Valid @RequestBody ConsultationRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierEcriture(utilisateur, request.getPatientId());
        // Le medecin auteur est toujours l'utilisateur connecte.
        request.setMedecinId(utilisateur.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(consultationService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une consultation (son auteur, s'il a toujours acces en ecriture, ou un administrateur)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        if (utilisateur.getRole() != Role.ADMIN) {
            ConsultationResponse consultation = consultationService.findById(id);
            if (!utilisateur.getId().equals(consultation.getMedecinId())) {
                throw new AccessDeniedException("Seul l'auteur de la consultation peut la supprimer");
            }
            carnetAccesService.verifierEcriture(utilisateur, consultation.getPatientId());
        }
        consultationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
