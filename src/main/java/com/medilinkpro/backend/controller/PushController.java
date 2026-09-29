package com.medilinkpro.backend.controller;

import com.medilinkpro.backend.dto.request.AbonnementPushRequest;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.service.push.CleVapidService;
import com.medilinkpro.backend.service.push.NotificationPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Tag(name = "Notifications push", description = "Abonnement des appareils aux notifications")
public class PushController {

    private final CleVapidService cleVapidService;
    private final NotificationPushService notificationPushService;

    @GetMapping("/cle-publique")
    @Operation(summary = "Cle publique VAPID a passer a PushManager.subscribe()")
    public Map<String, String> clePublique() {
        return Map.of("clePublique", cleVapidService.clePublique());
    }

    @PostMapping("/abonnements")
    @Operation(summary = "Enregistrer l'abonnement push de cet appareil pour l'utilisateur connecte")
    public ResponseEntity<Void> abonner(@Valid @RequestBody AbonnementPushRequest r, @AuthenticationPrincipal Utilisateur u) {
        notificationPushService.abonner(u.getId(), r.endpoint(), r.keys().p256dh(), r.keys().auth());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/abonnements")
    @Operation(summary = "Desactiver les notifications sur cet appareil")
    public ResponseEntity<Void> desabonner(@RequestParam String endpoint, @AuthenticationPrincipal Utilisateur u) {
        notificationPushService.desabonner(u.getId(), endpoint);
        return ResponseEntity.noContent().build();
    }
}
