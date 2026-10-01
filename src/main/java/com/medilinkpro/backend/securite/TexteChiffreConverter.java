package com.medilinkpro.backend.securite;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Chiffre en base un champ texte sensible (AES-256-GCM), de facon transparente pour le code.
 * A n'utiliser que sur des colonnes TEXT (le chiffre est plus long que le clair) et jamais
 * filtrees en SQL (le chiffrement est non deterministe).
 */
@Converter
public class TexteChiffreConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String valeur) {
        return ServiceChiffrement.instance().chiffrerTexte(valeur);
    }

    @Override
    public String convertToEntityAttribute(String valeur) {
        return ServiceChiffrement.instance().dechiffrerTexte(valeur);
    }
}
