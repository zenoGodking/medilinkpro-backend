package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.response.StatistiquesResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.StatistiquesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tableau de bord", description = "Statistiques d'activite (directeur : ses etablissements ; admin : plateforme)")
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    @GetMapping("/api/dashboard/statistiques")
    @Operation(summary = "Indicateurs des 30 derniers jours : rendez-vous, absences, teleconsultations, alertes, avis")
    public ResponseEntity<StatistiquesResponse> statistiques(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(statistiquesService.statistiques(utilisateur));
    }
}
