package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.SuiviSanteDto.*;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.SuiviSanteService;
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
@RequestMapping("/api/suivi")
@RequiredArgsConstructor
@Tag(name = "Suivi de sante", description = "Maladies chroniques (mesures), rappels de medicaments, vaccins, grossesse")
public class SuiviSanteController {

    private final SuiviSanteService service;

    // ---- Mesures
    @GetMapping("/patients/{patientId}/mesures")
    @Operation(summary = "Mesures d'un patient (tension, glycemie, poids, temperature, saturation)")
    public List<MesureResponse> mesures(@PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur u) {
        return service.mesures(patientId, u);
    }

    @PostMapping("/patients/{patientId}/mesures")
    @Operation(summary = "Ajouter une mesure (le patient lui-meme ou un medecin autorise)")
    public ResponseEntity<MesureResponse> ajouterMesure(@PathVariable UUID patientId, @Valid @RequestBody MesureRequest r,
                                                       @AuthenticationPrincipal Utilisateur u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouterMesure(patientId, r, u));
    }

    @DeleteMapping("/mesures/{id}")
    public ResponseEntity<Void> supprimerMesure(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur u) {
        service.supprimerMesure(id, u);
        return ResponseEntity.noContent().build();
    }

    // ---- Rappels de medicaments
    @GetMapping("/patients/{patientId}/rappels")
    public List<RappelResponse> rappels(@PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur u) {
        return service.rappels(patientId, u);
    }

    @PostMapping("/patients/{patientId}/rappels")
    @Operation(summary = "Creer un rappel de prise de medicament (le patient lui-meme)")
    public ResponseEntity<RappelResponse> ajouterRappel(@PathVariable UUID patientId, @Valid @RequestBody RappelRequest r,
                                                        @AuthenticationPrincipal Utilisateur u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouterRappel(patientId, r, u));
    }

    @PatchMapping("/rappels/{id}/basculer")
    public RappelResponse basculerRappel(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur u) {
        return service.basculerRappel(id, u);
    }

    @DeleteMapping("/rappels/{id}")
    public ResponseEntity<Void> supprimerRappel(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur u) {
        service.supprimerRappel(id, u);
        return ResponseEntity.noContent().build();
    }

    // ---- Vaccins
    @GetMapping("/patients/{patientId}/vaccinations")
    public List<VaccinationResponse> vaccinations(@PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur u) {
        return service.vaccinations(patientId, u);
    }

    @PostMapping("/patients/{patientId}/vaccinations")
    @Operation(summary = "Ajouter un vaccin : declare par le patient, ou valide s'il est saisi par un medecin autorise")
    public ResponseEntity<VaccinationResponse> ajouterVaccination(@PathVariable UUID patientId, @Valid @RequestBody VaccinationRequest r,
                                                                  @AuthenticationPrincipal Utilisateur u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouterVaccination(patientId, r, u));
    }

    @PatchMapping("/vaccinations/{id}/valider")
    @Operation(summary = "Un medecin autorise valide un vaccin declare par le patient")
    public VaccinationResponse validerVaccination(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur u) {
        return service.validerVaccination(id, u);
    }

    @DeleteMapping("/vaccinations/{id}")
    public ResponseEntity<Void> supprimerVaccination(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur u) {
        service.supprimerVaccination(id, u);
        return ResponseEntity.noContent().build();
    }

    // ---- Grossesse
    @GetMapping("/patients/{patientId}/grossesses")
    public List<GrossesseResponse> grossesses(@PathVariable UUID patientId, @AuthenticationPrincipal Utilisateur u) {
        return service.grossesses(patientId, u);
    }

    @PostMapping("/patients/{patientId}/grossesses")
    @Operation(summary = "Declarer une grossesse (date des dernieres regles)")
    public ResponseEntity<GrossesseResponse> declarerGrossesse(@PathVariable UUID patientId, @Valid @RequestBody GrossesseRequest r,
                                                               @AuthenticationPrincipal Utilisateur u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.declarerGrossesse(patientId, r, u));
    }

    @PatchMapping("/grossesses/{id}/terminer")
    public GrossesseResponse terminerGrossesse(@PathVariable UUID id, @Valid @RequestBody FinGrossesseRequest r,
                                               @AuthenticationPrincipal Utilisateur u) {
        return service.terminerGrossesse(id, r, u);
    }

    @PostMapping("/grossesses/{id}/visites")
    @Operation(summary = "Enregistrer une consultation prenatale (medecin autorise)")
    public GrossesseResponse ajouterVisite(@PathVariable UUID id, @Valid @RequestBody VisitePrenataleRequest r,
                                           @AuthenticationPrincipal Utilisateur u) {
        return service.ajouterVisite(id, r, u);
    }
}
