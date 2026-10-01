package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.response.NotificationAppResponse;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications de l'utilisateur connecte (cloche de l'application)")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "50 dernieres notifications de l'utilisateur connecte")
    public ResponseEntity<List<NotificationAppResponse>> lister(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(notificationService.lister(utilisateur.getId()));
    }

    @GetMapping("/non-lues")
    @Operation(summary = "Nombre de notifications non lues")
    public ResponseEntity<Map<String, Long>> nonLues(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(Map.of("nombre", notificationService.nombreNonLues(utilisateur.getId())));
    }

    @PatchMapping("/{id}/lue")
    public ResponseEntity<Void> marquerLue(@PathVariable UUID id, @AuthenticationPrincipal Utilisateur utilisateur) {
        notificationService.marquerLue(id, utilisateur.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/lues")
    @Operation(summary = "Tout marquer comme lu")
    public ResponseEntity<Void> toutesLues(@AuthenticationPrincipal Utilisateur utilisateur) {
        notificationService.marquerToutesLues(utilisateur.getId());
        return ResponseEntity.noContent().build();
    }
}
