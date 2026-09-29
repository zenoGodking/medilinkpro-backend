package com.medilinkpro.backend.enums;

/** Raison pour laquelle un medecin peut ecrire dans le carnet d'un patient. */
public enum MotifEcriture {
    /** Le patient a explicitement autorise ce medecin. */
    AUTORISATION_PATIENT,
    /** Le medecin a deja suivi ce patient (consultation, ou rendez-vous confirme/termine). */
    ANCIEN_PATIENT
}
