package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.response.CarteUrgenceResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.CarteUrgenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/carte-urgence")
@RequiredArgsConstructor
@Tag(name = "Carte d'urgence", description = "QR code d'urgence du patient (ecran de verrouillage, carte imprimee)")
public class CarteUrgenceController {

    private final CarteUrgenceService carteUrgenceService;

    @GetMapping("/moi")
    @Operation(summary = "Patient : jeton de sa carte d'urgence (a encoder dans le QR code)")
    public ResponseEntity<Map<String, String>> maCarte(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(Map.of("jeton", carteUrgenceService.monJeton(utilisateur)));
    }

    @PostMapping("/moi/regenerer")
    @Operation(summary = "Patient : nouveau QR code, l'ancien ne fonctionne plus (carte perdue)")
    public ResponseEntity<Map<String, String>> regenerer(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(Map.of("jeton", carteUrgenceService.regenerer(utilisateur)));
    }

    @GetMapping("/{jeton}")
    @Operation(summary = "Scanner une carte d'urgence (tout utilisateur connecte)")
    public ResponseEntity<CarteUrgenceResponse> consulter(
            @PathVariable String jeton, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(carteUrgenceService.consulter(jeton, utilisateur));
    }
}
