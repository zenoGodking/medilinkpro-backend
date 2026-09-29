package com.medilinkpro.backend.service.sms;

/**
 * Passerelle d'envoi reel de SMS (Twilio, Africa's Talking, operateur local...).
 * Aucune implementation n'est fournie : declarer un bean implementant cette interface
 * suffit a activer l'envoi reel (voir NotificationSmsService).
 */
public interface FournisseurSms {

    void envoyer(String numero, String message);
}
