package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.OrdonnanceRequest;
import com.medilinkpro.backend.dto.response.ConsultationResponse;
import com.medilinkpro.backend.dto.response.OrdonnanceResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.CarnetAccesService;
import com.medilinkpro.backend.service.ConsultationService;
import com.medilinkpro.backend.service.OrdonnanceService;
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
@RequestMapping("/api/ordonnances")
@RequiredArgsConstructor
@Tag(name = "Ordonnances", description = "Ordonnances numeriques avec code QR verifiable (Module 1 - F05)")
public class OrdonnanceController {

    private final OrdonnanceService ordonnanceService;
    private final ConsultationService consultationService;
    private final CarnetAccesService carnetAccesService;

    @GetMapping
    @Operation(summary = "Lister toutes les ordonnances (medecins et administrateurs)")
    public ResponseEntity<List<OrdonnanceResponse>> findAll(@AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLectureGlobale(utilisateur);
        return ResponseEntity.ok(ordonnanceService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une ordonnance par son id")
    public ResponseEntity<OrdonnanceResponse> findById(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        OrdonnanceResponse ordonnance = ordonnanceService.findById(id);
        carnetAccesService.verifierLecture(utilisateur, ordonnance.getPatientId());
        return ResponseEntity.ok(ordonnance);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Lister les ordonnances d'un patient")
    public ResponseEntity<List<OrdonnanceResponse>> findByPatient(
            @PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLecture(utilisateur, patientId);
        return ResponseEntity.ok(ordonnanceService.findByPatient(patientId));
    }

    @PostMapping
    @Operation(summary = "Generer une ordonnance pour une de ses consultations (medecin autorise)")
    public ResponseEntity<OrdonnanceResponse> create(
            @Valid @RequestBody OrdonnanceRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        ConsultationResponse consultation = consultationService.findById(request.getConsultationId());
        if (!utilisateur.getId().equals(consultation.getMedecinId())) {
            throw new AccessDeniedException("Seul le medecin de la consultation peut emettre l'ordonnance");
        }
        carnetAccesService.verifierEcriture(utilisateur, consultation.getPatientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ordonnanceService.create(request));
    }
}
