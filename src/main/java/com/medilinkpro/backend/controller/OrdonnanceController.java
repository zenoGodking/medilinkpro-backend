package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.OrdonnanceRequest;
import com.medilinkpro.backend.dto.response.OrdonnanceResponse;
import com.medilinkpro.backend.service.OrdonnanceService;
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
@RequestMapping("/api/ordonnances")
@RequiredArgsConstructor
@Tag(name = "Ordonnances", description = "Ordonnances numeriques avec code QR verifiable (Module 1 - F05)")
public class OrdonnanceController {

    private final OrdonnanceService ordonnanceService;

    @GetMapping
    @Operation(summary = "Lister toutes les ordonnances")
    public ResponseEntity<List<OrdonnanceResponse>> findAll() {
        return ResponseEntity.ok(ordonnanceService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer une ordonnance par son id")
    public ResponseEntity<OrdonnanceResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ordonnanceService.findById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Lister les ordonnances d'un patient")
    public ResponseEntity<List<OrdonnanceResponse>> findByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(ordonnanceService.findByPatient(patientId));
    }

    @PostMapping
    @Operation(summary = "Generer une ordonnance numerique pour une consultation")
    public ResponseEntity<OrdonnanceResponse> create(@Valid @RequestBody OrdonnanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordonnanceService.create(request));
    }
}
