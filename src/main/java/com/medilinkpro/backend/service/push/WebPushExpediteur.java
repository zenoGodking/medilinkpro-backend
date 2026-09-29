package com.medilinkpro.backend.service.push;

import com.medilinkpro.backend.entity.AbonnementPush;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Envoi reel via le protocole Web Push (VAPID + chiffrement aes128gcm). */
@Component
@RequiredArgsConstructor
public class WebPushExpediteur implements ExpediteurPush {

    private final CleVapidService cleVapidService;

    @Value("${medilinkpro.push.sujet:mailto:contact@medilinkpro.cm}")
    private String sujet;

    private volatile PushService pushService;

    @Override
    public int envoyer(AbonnementPush abonnement, String contenuJson) throws Exception {
        Notification notification = new Notification(
                abonnement.getEndpoint(), abonnement.getP256dh(), abonnement.getAuth(), contenuJson);
        return service().send(notification).getStatusLine().getStatusCode();
    }

    private PushService service() throws Exception {
        if (pushService == null) {
            synchronized (this) {
                if (pushService == null) {
                    String[] cles = cleVapidService.cles();
                    pushService = new PushService(cles[0], cles[1], sujet);
                }
            }
        }
        return pushService;
    }
}
