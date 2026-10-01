package com.medilinkpro.backend.enums;

/**
 * Statut d'un rendez-vous au cours de son cycle de vie :
 * EN_ATTENTE (demande du patient) -> CONFIRME (accepte ou reporte par le medecin) -> TERMINE / NO_SHOW,
 * ou REFUSE (par le medecin) / ANNULE (par le patient ou le medecin).
 */
public enum StatutRendezVous {
    EN_ATTENTE,
    CONFIRME,
    ANNULE,
    TERMINE,
    NO_SHOW,
    REFUSE;

    /** Statuts qui liberent le creneau. */
    public static final java.util.Set<StatutRendezVous> LIBERANT_LE_CRENEAU = java.util.Set.of(ANNULE, REFUSE);
}
