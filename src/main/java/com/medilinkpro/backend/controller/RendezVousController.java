package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.RendezVousRequest;
import com.medilinkpro.backend.dto.request.StatutRendezVousRequest;
import com.medilinkpro.backend.dto.response.RendezVousResponse;
import com.medilinkpro.backend.service.RendezVousService;
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
@RequestMapping("/api/rendez-vous")
@RequiredArgsConstructor
@Tag(name = "Rendez-vous", description = "Prise de rendez-vous en ligne (Module 2 - F13 a F18)")
public class RendezVousController {

    private final RendezVousService rendezVousService;

    @GetMapping
    @Operation(summary = "Lister tous les rendez-vous")
    public ResponseEntity<List<RendezVousResponse>> findAll() {
        return ResponseEntity.ok(rendezVousService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un rendez-vous par son id")
    public ResponseEntity<RendezVousResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(rendezVousService.findById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Lister les rendez-vous d'un patient")
    public ResponseEntity<List<RendezVousResponse>> findByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(rendezVousService.findByPatient(patientId));
    }

    @GetMapping("/medecin/{medecinId}")
    @Operation(summary = "Lister les rendez-vous d'un medecin (agenda)")
    public ResponseEntity<List<RendezVousResponse>> findByMedecin(@PathVariable UUID medecinId) {
        return ResponseEntity.ok(rendezVousService.findByMedecin(medecinId));
    }

    @PostMapping
    @Operation(
            summary = "Prendre un rendez-vous (F13)",
            description = "Verifie la disponibilite du creneau puis confirme le rendez-vous"
    )
    public ResponseEntity<RendezVousResponse> create(@Valid @RequestBody RendezVousRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rendezVousService.create(request));
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Changer le statut d'un rendez-vous (annulation, confirmation, etc.)")
    public ResponseEntity<RendezVousResponse> updateStatut(@PathVariable UUID id, @Valid @RequestBody StatutRendezVousRequest request) {
        return ResponseEntity.ok(rendezVousService.updateStatut(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un rendez-vous")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        rendezVousService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
