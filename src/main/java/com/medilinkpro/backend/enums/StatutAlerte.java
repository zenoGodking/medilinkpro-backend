package com.medilinkpro.backend.enums;

/**
 * Cycle de vie d'une alerte de soins a domicile envoyee par un patient.
 *
 * EN_ATTENTE   -> une infirmiere repond "present"        -> REPONDUE
 * REPONDUE     -> l'infirmiere se retracte (imprevu)      -> EN_ATTENTE
 * REPONDUE     -> l'infirmiere envoie son compte-rendu     -> SERVICE_RENDU
 * SERVICE_RENDU -> le patient note l'intervention          -> TERMINEE
 *
 * Une infirmiere ne peut avoir qu'une seule alerte REPONDUE a la fois : elle doit
 * soumettre son compte-rendu (passage a SERVICE_RENDU) avant de pouvoir repondre
 * a une nouvelle alerte.
 */
public enum StatutAlerte {
    EN_ATTENTE,
    REPONDUE,
    SERVICE_RENDU,
    TERMINEE,
    ANNULEE
}
