package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.DeclarationDecesRequest;
import com.medilinkpro.backend.dto.request.PatientUpdateRequest;
import com.medilinkpro.backend.dto.response.DecesResponse;
import com.medilinkpro.backend.dto.response.PatientResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.service.CarnetAccesService;
import com.medilinkpro.backend.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Patients. Acces (voir CarnetAccesService) : le patient pour lui-meme, tout medecin valide
 * en lecture, en ecriture uniquement sur les donnees medicales et s'il y est autorise.
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Gestion des patients et de leurs informations medicales de base")
public class PatientController {

    private final PatientService patientService;
    private final CarnetAccesService carnetAccesService;

    @GetMapping
    @Operation(summary = "Lister tous les patients (medecins et administrateurs)")
    public ResponseEntity<List<PatientResponse>> findAll(@AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLectureGlobale(utilisateur);
        return ResponseEntity.ok(patientService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Recuperer un patient par son id")
    public ResponseEntity<PatientResponse> findById(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.verifierLecture(utilisateur, id);
        return ResponseEntity.ok(patientService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre a jour un patient",
            description = "Le patient modifie toute sa fiche ; un medecin autorise ne modifie que les donnees medicales.")
    public ResponseEntity<PatientResponse> update(
            @PathVariable UUID id, @Valid @RequestBody PatientUpdateRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        if (utilisateur.getRole() == Role.ADMIN
                || (utilisateur.getRole() == Role.PATIENT && utilisateur.getId().equals(id))) {
            return ResponseEntity.ok(patientService.update(id, request));
        }
        carnetAccesService.verifierEcriture(utilisateur, id);
        return ResponseEntity.ok(patientService.updateDonneesMedicales(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un patient (le patient lui-meme ou un administrateur)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        if (utilisateur.getRole() != Role.ADMIN && !utilisateur.getId().equals(id)) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que votre propre compte");
        }
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deces")
    @Operation(summary = "Declarer le deces d'un patient (tout medecin valide)",
            description = "Désactive le compte, annule les alertes en attente et informe le proche par SMS.")
    public ResponseEntity<DecesResponse> declarerDeces(
            @PathVariable UUID id, @Valid @RequestBody DeclarationDecesRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(carnetAccesService.declarerDeces(id, utilisateur, request));
    }

    @DeleteMapping("/{id}/deces")
    @Operation(summary = "Annuler une declaration de deces erronee (administrateur)")
    public ResponseEntity<Void> annulerDeces(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        carnetAccesService.annulerDeces(id, utilisateur);
        return ResponseEntity.noContent().build();
    }
}
