package com.medilinkpro.backend.enums;

/**
 * Roles applicatifs de MediLinkPro.
 * Utilise par Spring Security pour les autorisations par endpoint.
 */
public enum Role {
    PATIENT,
    MEDECIN,
    ADMIN,
    DIRECTEUR,
    SECRETAIRE,
    INFIRMIER
}
