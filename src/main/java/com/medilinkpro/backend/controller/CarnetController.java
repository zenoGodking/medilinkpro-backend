package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.AutorisationRequest;
import com.medilinkpro.backend.dto.response.AccesCarnetResponse;
import com.medilinkpro.backend.dto.response.AutorisationResponse;
import com.medilinkpro.backend.dto.response.CarnetResponse;
import com.medilinkpro.backend.dto.response.PatientAccessibleResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.service.CarnetAccesService;
import com.medilinkpro.backend.service.JournalAccesService;
import org.springframework.security.access.AccessDeniedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carnets")
@RequiredArgsConstructor
@Tag(name = "Carnets medicaux", description = "Lecture des carnets et autorisations d'ecriture donnees par les patients")
public class CarnetController {

    private final CarnetAccesService carnetAccesService;
    private final JournalAccesService journalAccesService;

    @GetMapping("/journal")
    @Operation(summary = "Patient : qui a consulte ou modifie mon carnet (200 derniers acces)")
    public ResponseEntity<List<AccesCarnetResponse>> journal(@AuthenticationPrincipal Utilisateur utilisateur) {
        if (utilisateur.getRole() != Role.PATIENT) {
            throw new AccessDeniedException("Réservé au patient");
        }
        return ResponseEntity.ok(journalAccesService.journal(utilisateur.getId()));
    }

    @GetMapping("/{patientId}")
    @Operation(summary = "Carnet complet d'un patient (le patient lui-meme, tout medecin, admin), avec les droits d'ecriture du medecin")
    public ResponseEntity<CarnetResponse> carnet(@PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(carnetAccesService.carnet(patientId, utilisateur));
    }

    @GetMapping("/ecriture-autorisee")
    @Operation(summary = "Medecin : patients dans le carnet desquels il peut ecrire (autorisations + anciens patients)")
    public ResponseEntity<List<PatientAccessibleResponse>> ecritureAutorisee(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(carnetAccesService.patientsAccessiblesEnEcriture(utilisateur));
    }

    @GetMapping("/autorisations")
    @Operation(summary = "Patient : medecins autorises a ecrire dans son carnet")
    public ResponseEntity<List<AutorisationResponse>> mesAutorisations(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(carnetAccesService.mesAutorisations(utilisateur));
    }

    @PostMapping("/autorisations")
    @Operation(summary = "Patient : autoriser un medecin a ecrire dans son carnet")
    public ResponseEntity<AutorisationResponse> autoriser(
            @Valid @RequestBody AutorisationRequest request, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(carnetAccesService.autoriser(utilisateur, request.getMedecinId()));
    }

    @DeleteMapping("/autorisations/{medecinId}")
    @Operation(summary = "Patient : retirer l'autorisation d'ecriture d'un medecin")
    public ResponseEntity<Void> revoquer(@PathVariable UUID medecinId, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.revoquer(utilisateur, medecinId);
        return ResponseEntity.noContent().build();
    }
}
