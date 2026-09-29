package com.medilinkpro.backend.enums;

/** Lecture indicative d'une mesure : ne remplace pas un avis medical. */
public enum NiveauMesure {
    NORMAL,
    ATTENTION,
    ALERTE,
    /** Pas de seuil defini pour ce type (ex: poids). */
    NON_EVALUE
}
