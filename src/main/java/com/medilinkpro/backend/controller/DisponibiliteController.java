package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.AbsenceRequest;
import com.medilinkpro.backend.dto.request.DisponibilitesRequest;
import com.medilinkpro.backend.dto.response.CreneauResponse;
import com.medilinkpro.backend.dto.response.DisponibilitesResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.DisponibiliteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disponibilites")
@RequiredArgsConstructor
@Tag(name = "Disponibilites medecin", description = "Semaine type, absences et creneaux reservables d'un medecin")
public class DisponibiliteController {

    private final DisponibiliteService disponibiliteService;

    @GetMapping("/medecins/{medecinId}")
    @Operation(summary = "Semaine type et absences a venir d'un medecin (heures ouvrables par defaut si non definies)")
    public ResponseEntity<DisponibilitesResponse> disponibilites(@PathVariable UUID medecinId) {
        return ResponseEntity.ok(disponibiliteService.disponibilites(medecinId));
    }

    @GetMapping("/medecins/{medecinId}/creneaux")
    @Operation(summary = "Creneaux d'un medecin entre deux dates (31 jours max), avec leur etat libre/pris")
    public ResponseEntity<List<CreneauResponse>> creneaux(
            @PathVariable UUID medecinId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return ResponseEntity.ok(disponibiliteService.creneaux(medecinId, du, au));
    }

    @PutMapping("/moi")
    @Operation(summary = "Medecin : definir sa semaine type (remplace toutes ses plages)")
    public ResponseEntity<DisponibilitesResponse> definirSemaine(
            @Valid @RequestBody DisponibilitesRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(disponibiliteService.definirSemaine(utilisateur.getId(), request));
    }

    @PostMapping("/moi/absences")
    @Operation(summary = "Medecin : declarer une absence (conges, formation...)")
    public ResponseEntity<DisponibilitesResponse.Absence> ajouterAbsence(
            @Valid @RequestBody AbsenceRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disponibiliteService.ajouterAbsence(utilisateur.getId(), request));
    }

    @DeleteMapping("/moi/absences/{absenceId}")
    @Operation(summary = "Medecin : supprimer une de ses absences")
    public ResponseEntity<Void> supprimerAbsence(@PathVariable UUID absenceId, @AuthenticationPrincipal Utilisateur utilisateur) {
        disponibiliteService.supprimerAbsence(utilisateur.getId(), absenceId);
        return ResponseEntity.noContent().build();
    }
}
