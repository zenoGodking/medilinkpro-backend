package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.response.DossierMedicalResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.CarnetAccesService;
import com.medilinkpro.backend.service.DossierMedicalService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dossiers-medicaux")
@RequiredArgsConstructor
@Tag(name = "Dossiers Medicaux", description = "Dossier Medical Electronique - DME (Module 1 - F01 a F09)")
public class DossierMedicalController {

    private final DossierMedicalService dossierMedicalService;
    private final CarnetAccesService carnetAccesService;

    @GetMapping
    @Operation(summary = "Lister tous les dossiers medicaux")
    public ResponseEntity<List<DossierMedicalResponse>> findAll(@AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLectureGlobale(utilisateur);
        return ResponseEntity.ok(dossierMedicalService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un dossier medical par son id")
    public ResponseEntity<DossierMedicalResponse> findById(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        DossierMedicalResponse dossier = dossierMedicalService.findById(id);
        carnetAccesService.verifierLecture(utilisateur, dossier.getPatientId());
        return ResponseEntity.ok(dossier);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Recuperer le dossier medical d'un patient (F02)")
    public ResponseEntity<DossierMedicalResponse> findByPatient(
            @PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLecture(utilisateur, patientId);
        return ResponseEntity.ok(dossierMedicalService.findByPatientId(patientId));
    }
}
