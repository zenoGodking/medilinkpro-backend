package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.ConsultationRequest;
import com.medilinkpro.backend.dto.response.ConsultationResponse;
import com.medilinkpro.backend.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Tag(name = "Consultations", description = "Comptes rendus medicaux, diagnostics (Module 1 - F03)")
public class ConsultationController {

    private final ConsultationService consultationService;

    @GetMapping
    @Operation(summary = "Lister toutes les consultations")
    public ResponseEntity<List<ConsultationResponse>> findAll() {
        return ResponseEntity.ok(consultationService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une consultation par son id")
    public ResponseEntity<ConsultationResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(consultationService.findById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Lister les consultations d'un patient")
    public ResponseEntity<List<ConsultationResponse>> findByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(consultationService.findByPatient(patientId));
    }

    @GetMapping("/medecin/{medecinId}")
    @Operation(summary = "Lister les consultations realisees par un medecin")
    public ResponseEntity<List<ConsultationResponse>> findByMedecin(@PathVariable UUID medecinId) {
        return ResponseEntity.ok(consultationService.findByMedecin(medecinId));
    }

    @PostMapping
    @Operation(summary = "Enregistrer une nouvelle consultation")
    public ResponseEntity<ConsultationResponse> create(@Valid @RequestBody ConsultationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultationService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une consultation")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        consultationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
