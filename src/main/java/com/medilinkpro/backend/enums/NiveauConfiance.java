package com.medilinkpro.backend.enums;

/**
 * Niveau de confiance d'une correspondance faciale. Ce n'est jamais une identification
 * certaine : meme ELEVEE, la correspondance doit etre confirmee visuellement (photo de reference).
 */
public enum NiveauConfiance {
    ELEVEE,
    MOYENNE,
    FAIBLE
}
