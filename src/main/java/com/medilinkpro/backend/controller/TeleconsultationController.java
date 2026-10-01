package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.SignalTeleconsultationRequest;
import com.medilinkpro.backend.dto.response.TeleconsultationResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.TeleconsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * Teleconsultation video : fiche de la salle (REST) et relais de signalisation WebRTC (STOMP,
 * destination /app/teleconsultation/{rendezVousId}/signal, reponses sur /user/queue/teleconsultation).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teleconsultation", description = "Salle video medecin-patient d'un rendez-vous en teleconsultation")
public class TeleconsultationController {

    private final TeleconsultationService teleconsultationService;

    @GetMapping("/api/teleconsultations/{rendezVousId}")
    @Operation(summary = "Etat de la salle de teleconsultation d'un rendez-vous (son medecin ou son patient)")
    public ResponseEntity<TeleconsultationResponse> infos(
            @PathVariable UUID rendezVousId, @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(teleconsultationService.infos(rendezVousId, utilisateur));
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/teleconsultations/{rendezVousId}/cloture")
    @Operation(summary = "Medecin : enregistrer le compte rendu et, si besoin, l'ordonnance de la teleconsultation (cloture le rendez-vous)")
    public ResponseEntity<com.medilinkpro.backend.dto.response.ClotureTeleconsultationResponse> cloturer(
            @PathVariable UUID rendezVousId,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
            com.medilinkpro.backend.dto.request.ClotureTeleconsultationRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(teleconsultationService.cloturer(rendezVousId, utilisateur, request));
    }

    @MessageMapping("/teleconsultation/{rendezVousId}/signal")
    public void signal(@DestinationVariable UUID rendezVousId, @Payload SignalTeleconsultationRequest signal,
                       Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof Utilisateur u)) {
            return;
        }
        try {
            teleconsultationService.relayer(rendezVousId, u, signal);
        } catch (RuntimeException e) {
            log.debug("Signal de téléconsultation ignore ({}) : {}", rendezVousId, e.getMessage());
        }
    }
}
