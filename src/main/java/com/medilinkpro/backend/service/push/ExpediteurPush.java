package com.medilinkpro.backend.service.push;

import com.medilinkpro.backend.entity.AbonnementPush;

/** Envoi effectif d'un message push chiffre a un appareil. Retourne le code HTTP du service push. */
public interface ExpediteurPush {

    int envoyer(AbonnementPush abonnement, String contenuJson) throws Exception;
}
