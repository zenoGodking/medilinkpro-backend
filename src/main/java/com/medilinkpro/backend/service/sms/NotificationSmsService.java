package com.medilinkpro.backend.service.sms;

import com.medilinkpro.backend.entity.NotificationSms;
import com.medilinkpro.backend.repository.NotificationSmsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Envoie un SMS via le FournisseurSms configure et en garde une trace en base.
 * Tant qu'aucun fournisseur n'est branche, le message est seulement journalise et
 * enregistre avec le statut NON_ENVOYE_AUCUN_FOURNISSEUR : rien ne part reellement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSmsService {

    private final NotificationSmsRepository notificationSmsRepository;
    private final ObjectProvider<FournisseurSms> fournisseurSms;

    public NotificationSms envoyer(String numero, String message, String contexte, UUID patientId) {
        NotificationSms.Statut statut;
        FournisseurSms fournisseur = fournisseurSms.getIfAvailable();
        if (fournisseur == null) {
            log.warn("SMS NON ENVOYE (aucun fournisseur configure) a {} : {}", numero, message);
            statut = NotificationSms.Statut.NON_ENVOYE_AUCUN_FOURNISSEUR;
        } else {
            try {
                fournisseur.envoyer(numero, message);
                statut = NotificationSms.Statut.ENVOYE;
            } catch (RuntimeException e) {
                log.error("Echec d'envoi du SMS a {}", numero, e);
                statut = NotificationSms.Statut.ECHEC;
            }
        }
        return notificationSmsRepository.save(NotificationSms.builder()
                .destinataire(numero)
                .message(message)
                .contexte(contexte)
                .patientId(patientId)
                .statut(statut)
                .build());
    }
}
