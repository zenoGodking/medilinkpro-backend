package com.medilinkpro.backend.enums;

/**
 * Statut de validation d'un compte professionnel (Medecin, Secretaire, Directeur).
 * Un compte EN_ATTENTE ne peut pas se connecter tant qu'un Admin ne l'a pas APPROUVE.
 * Patient et Admin sont APPROUVE des l'inscription (pas de validation requise).
 */
public enum StatutCompte {
    EN_ATTENTE,
    APPROUVE,
    REJETE
}
