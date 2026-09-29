package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.response.VerificationOrdonnanceResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.PharmacieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacie")
@RequiredArgsConstructor
@Tag(name = "Pharmacie", description = "Verification et delivrance des ordonnances par QR code")
public class PharmacieController {

    private final PharmacieService pharmacieService;

    @GetMapping("/ordonnances/{ordonnanceId}/qr")
    @Operation(summary = "Patient : QR code d'une de ses ordonnances, a presenter en pharmacie")
    public Map<String, Object> qr(@PathVariable UUID ordonnanceId, @AuthenticationPrincipal Utilisateur u) {
        return pharmacieService.qrPatient(ordonnanceId, u);
    }

    @GetMapping("/verifier/{jeton}")
    @Operation(summary = "Pharmacien : verifier une ordonnance scannee (authenticite, validite, deja delivree ?)")
    public VerificationOrdonnanceResponse verifier(@PathVariable String jeton, @AuthenticationPrincipal Utilisateur u) {
        return pharmacieService.verifier(jeton, u);
    }

    @PostMapping("/delivrer/{jeton}")
    @Operation(summary = "Pharmacien : delivrer l'ordonnance (une seule fois, avant expiration)")
    public VerificationOrdonnanceResponse delivrer(@PathVariable String jeton, @AuthenticationPrincipal Utilisateur u) {
        return pharmacieService.delivrer(jeton, u);
    }

    @GetMapping("/mes-delivrances")
    @Operation(summary = "Pharmacien : historique de ses delivrances")
    public List<VerificationOrdonnanceResponse> mesDelivrances(@AuthenticationPrincipal Utilisateur u) {
        return pharmacieService.mesDelivrances(u);
    }
}
