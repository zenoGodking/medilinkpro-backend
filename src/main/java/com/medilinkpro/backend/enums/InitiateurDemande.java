package com.medilinkpro.backend.enums;

/**
 * Qui est a l'origine d'une demande d'integration d'un medecin dans un etablissement.
 * Determine qui doit y repondre : si c'est l'etablissement qui invite, le medecin
 * doit valider ; si c'est le medecin qui demande, l'etablissement (Directeur/Admin) doit valider.
 */
public enum InitiateurDemande {
    ETABLISSEMENT,
    MEDECIN
}
