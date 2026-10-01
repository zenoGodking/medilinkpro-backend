package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.response.NotificationAppResponse;
import com.medilinkpro.backend.entity.NotificationApp;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.NotificationAppRepository;
import com.medilinkpro.backend.service.push.NotificationPushService;
import com.medilinkpro.backend.service.sms.NotificationSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

/**
 * Point d'entree unique pour prevenir un utilisateur : notification dans l'application (toujours),
 * temps reel (/user/queue/notifications), push si l'appareil est abonne, et SMS si demande.
 * Les envois externes partent apres la validation de la transaction, pour ne jamais annoncer
 * un changement qui serait finalement annule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String QUEUE = "/queue/notifications";

    private final NotificationAppRepository repository;
    private final NotificationPushService pushService;
    private final NotificationSmsService smsService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void notifier(Utilisateur destinataire, String titre, String message, String lien, boolean aussiParSms) {
        NotificationApp notification = repository.save(NotificationApp.builder()
                .destinataireId(destinataire.getId())
                .titre(titre)
                .message(message)
                .lien(lien)
                .build());
        NotificationAppResponse response = toResponse(notification);
        String email = destinataire.getEmail();
        String telephone = destinataire.getTelephone();
        apresCommit(() -> {
            try {
                messagingTemplate.convertAndSendToUser(email, QUEUE, response);
                pushService.envoyer(destinataire.getId(), titre, message, lien, "notif-" + notification.getId());
                if (aussiParSms && telephone != null && !telephone.isBlank()) {
                    smsService.envoyer(telephone, "MediLinkPro : " + message, "NOTIFICATION", null);
                }
            } catch (RuntimeException e) {
                // La notification reste disponible dans l'application meme si un canal externe echoue.
                log.warn("Canal de notification indisponible pour {} : {}", destinataire.getId(), e.getMessage());
            }
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationAppResponse> lister(UUID utilisateurId) {
        return repository.findByDestinataireIdOrderByDateCreationDesc(utilisateurId, PageRequest.of(0, 50))
                .stream().map(NotificationService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long nombreNonLues(UUID utilisateurId) {
        return repository.countByDestinataireIdAndLueFalse(utilisateurId);
    }

    @Transactional
    public void marquerLue(UUID notificationId, UUID utilisateurId) {
        NotificationApp n = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        if (!n.getDestinataireId().equals(utilisateurId)) {
            throw new AccessDeniedException("Cette notification ne vous est pas destinée");
        }
        n.setLue(true);
    }

    @Transactional
    public void marquerToutesLues(UUID utilisateurId) {
        repository.marquerToutesLues(utilisateurId);
    }

    private static NotificationAppResponse toResponse(NotificationApp n) {
        return NotificationAppResponse.builder()
                .id(n.getId()).titre(n.getTitre()).message(n.getMessage()).lien(n.getLien())
                .lue(n.isLue()).dateCreation(n.getDateCreation())
                .build();
    }

    private static void apresCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
