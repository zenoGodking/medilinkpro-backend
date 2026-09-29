package com.medilinkpro.backend.service.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.entity.AbonnementPush;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.repository.AbonnementPushRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notifications push (application installee ou navigateur) : envoyees en arriere-plan pour ne
 * jamais ralentir une requete ; les abonnements expires (410/404) sont supprimes automatiquement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final AbonnementPushRepository abonnementRepository;
    private final ExpediteurPush expediteur;
    private final ObjectMapper objectMapper;
    @Qualifier("executeurPush")
    private final TaskExecutor executeurPush;

    @Transactional
    public void abonner(UUID utilisateurId, String endpoint, String p256dh, String auth) {
        AbonnementPush a = abonnementRepository.findByEndpoint(endpoint).orElseGet(AbonnementPush::new);
        a.setUtilisateurId(utilisateurId);
        a.setEndpoint(endpoint);
        a.setP256dh(p256dh);
        a.setAuth(auth);
        abonnementRepository.save(a);
    }

    @Transactional
    public void desabonner(UUID utilisateurId, String endpoint) {
        abonnementRepository.findByEndpoint(endpoint)
                .filter(a -> a.getUtilisateurId().equals(utilisateurId))
                .ifPresent(abonnementRepository::delete);
    }

    public void envoyer(UUID utilisateurId, String titre, String corps, String url, String tag) {
        diffuser(abonnementRepository.findByUtilisateurId(utilisateurId), titre, corps, url, tag);
    }

    public void envoyerAuRole(Role role, String titre, String corps, String url, String tag) {
        diffuser(abonnementRepository.findParRole(role), titre, corps, url, tag);
    }

    private void diffuser(List<AbonnementPush> abonnements, String titre, String corps, String url, String tag) {
        if (abonnements.isEmpty()) {
            return;
        }
        Map<String, String> message = new LinkedHashMap<>();
        message.put("titre", titre);
        message.put("corps", corps);
        message.put("url", url);
        message.put("tag", tag);
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }
        for (AbonnementPush a : abonnements) {
            executeurPush.execute(() -> envoyerUn(a, json));
        }
    }

    private void envoyerUn(AbonnementPush a, String json) {
        try {
            int statut = expediteur.envoyer(a, json);
            if (statut == 404 || statut == 410) {
                abonnementRepository.findByEndpoint(a.getEndpoint()).ifPresent(abonnementRepository::delete);
            } else if (statut >= 400) {
                log.warn("Notification push refusee ({}) pour l'utilisateur {}", statut, a.getUtilisateurId());
            }
        } catch (Exception e) {
            log.warn("Echec d'envoi push a l'utilisateur {} : {}", a.getUtilisateurId(), e.getMessage());
        }
    }
}
