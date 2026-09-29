package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.PositionRequest;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.service.AlerteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Canal WebSocket de partage de position des infirmieres : l'application envoie
 * ses coordonnees GPS sur "/app/infirmiers/position" a chaque deplacement significatif.
 * Plus leger qu'un appel REST repete pendant le trajet vers le patient.
 */
@Controller
@RequiredArgsConstructor
public class PositionInfirmierController {

    private final AlerteService alerteService;

    @MessageMapping("/infirmiers/position")
    public void partagerPosition(@Valid @Payload PositionRequest position, Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof Utilisateur u
                && u.getRole() == Role.INFIRMIER) {
            alerteService.mettreAJourPosition(u.getId(), position);
        }
    }
}
