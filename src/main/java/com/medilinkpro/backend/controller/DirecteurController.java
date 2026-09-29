package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.response.EtablissementResponse;
import com.medilinkpro.backend.dto.response.PatientEtablissementResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.DirecteurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directeur")
@RequiredArgsConstructor
@Tag(name = "Espace directeur", description = "Etablissements du directeur et patients qui y ont ete recus")
public class DirecteurController {

    private final DirecteurService directeurService;

    @GetMapping("/etablissements")
    @Operation(summary = "Etablissements dont le directeur connecte est responsable")
    public ResponseEntity<List<EtablissementResponse>> mesEtablissements(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(directeurService.mesEtablissements(utilisateur));
    }

    @GetMapping("/patients")
    @Operation(summary = "Patients ayant eu rendez-vous dans ses etablissements (identite seulement, aucune donnee medicale)")
    public ResponseEntity<List<PatientEtablissementResponse>> mesPatients(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(directeurService.mesPatients(utilisateur));
    }
}
