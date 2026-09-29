package com.medilinkpro.backend.util;

import com.medilinkpro.backend.exception.BadRequestException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Empreinte faciale : vecteur de 128 reels produit par le modele face_recognition de face-api
 * (calcule cote navigateur). Deux visages de la meme personne donnent en general une distance
 * euclidienne inferieure a ~0.5 ; au-dela de 0.6 on considere qu'il ne s'agit pas de la meme personne.
 */
public final class DescripteurFacial {

    public static final int DIMENSION = 128;

    private DescripteurFacial() {
    }

    public static double[] valider(List<Double> valeurs) {
        if (valeurs == null || valeurs.size() != DIMENSION) {
            throw new BadRequestException("Empreinte faciale invalide : " + DIMENSION + " valeurs attendues");
        }
        double[] vecteur = new double[DIMENSION];
        for (int i = 0; i < DIMENSION; i++) {
            Double v = valeurs.get(i);
            if (v == null || !Double.isFinite(v) || Math.abs(v) > 1.0) {
                throw new BadRequestException("Empreinte faciale invalide : valeur hors bornes");
            }
            vecteur[i] = v;
        }
        return vecteur;
    }

    public static String serialiser(double[] vecteur) {
        return Arrays.stream(vecteur)
                .mapToObj(v -> String.format(Locale.ROOT, "%.6f", v))
                .collect(Collectors.joining(","));
    }

    public static double[] deserialiser(String texte) {
        String[] parties = texte.split(",");
        double[] vecteur = new double[parties.length];
        for (int i = 0; i < parties.length; i++) {
            vecteur[i] = Double.parseDouble(parties[i]);
        }
        return vecteur;
    }

    public static double distance(double[] a, double[] b) {
        if (a.length != b.length) {
            return Double.MAX_VALUE;
        }
        double somme = 0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            somme += d * d;
        }
        return Math.sqrt(somme);
    }
}
