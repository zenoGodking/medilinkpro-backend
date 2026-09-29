package com.medilinkpro.backend.enums;

/**
 * Niveau d'acces au carnet d'un patient identifie par reconnaissance faciale :
 * ESSENTIEL pour tout utilisateur connecte (le minimum pour agir),
 * COMPLET (lecture seule) pour le personnel de sante valide (Medecin, Infirmier).
 */
public enum NiveauAccesUrgence {
    ESSENTIEL,
    COMPLET
}
