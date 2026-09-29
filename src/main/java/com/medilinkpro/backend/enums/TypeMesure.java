package com.medilinkpro.backend.enums;

/**
 * Mesures de suivi des maladies chroniques. Bornes de plausibilite (rejet des erreurs de saisie)
 * et unites fixes : pas de conversion, l'unite est affichee a cote de chaque valeur.
 */
public enum TypeMesure {
    /** valeur = systolique, valeur2 = diastolique (mmHg). */
    TENSION("mmHg", 50, 260),
    /** Glycemie capillaire en g/L. */
    GLYCEMIE("g/L", 0.2, 6.0),
    POIDS("kg", 0.5, 400),
    TEMPERATURE("°C", 30, 45),
    SATURATION_O2("%", 50, 100);

    private final String unite;
    private final double min;
    private final double max;

    TypeMesure(String unite, double min, double max) {
        this.unite = unite;
        this.min = min;
        this.max = max;
    }

    public String getUnite() {
        return unite;
    }

    public boolean plausible(double valeur) {
        return valeur >= min && valeur <= max;
    }
}
