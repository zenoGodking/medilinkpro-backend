package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.ResultatAnalyseRequest;
import com.medilinkpro.backend.dto.response.ResultatAnalyseResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.service.CarnetAccesService;
import com.medilinkpro.backend.service.ResultatAnalyseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resultats-analyses")
@RequiredArgsConstructor
@Tag(name = "Resultats Analyses", description = "Import des resultats biologiques et d'imagerie (Module 1 - F04)")
public class ResultatAnalyseController {

    private final ResultatAnalyseService resultatAnalyseService;
    private final CarnetAccesService carnetAccesService;

    @GetMapping("/dossier/{dossierId}")
    @Operation(summary = "Lister les resultats d'analyses d'un dossier medical")
    public ResponseEntity<List<ResultatAnalyseResponse>> findByDossier(
            @PathVariable UUID dossierId, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLecture(utilisateur, resultatAnalyseService.patientIdDuDossier(dossierId));
        return ResponseEntity.ok(resultatAnalyseService.findByDossier(dossierId));
    }

    @PostMapping
    @Operation(summary = "Ajouter un resultat d'analyse (medecin autorise)")
    public ResponseEntity<ResultatAnalyseResponse> create(
            @Valid @RequestBody ResultatAnalyseRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierEcriture(utilisateur,
                resultatAnalyseService.patientIdDuDossier(request.getDossierMedicalId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resultatAnalyseService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un resultat d'analyse (medecin autorise ou administrateur)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        if (utilisateur.getRole() != Role.ADMIN) {
            carnetAccesService.verifierEcriture(utilisateur, resultatAnalyseService.patientIdDuResultat(id));
        }
        resultatAnalyseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
