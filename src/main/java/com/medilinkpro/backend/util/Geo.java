package com.medilinkpro.backend.util;

/** Calculs geographiques simples (coordonnees WGS84 en degres). */
public final class Geo {

    private static final double RAYON_TERRE_KM = 6371.0;

    private Geo() {
    }

    /** Distance a vol d'oiseau (formule de haversine), en kilometres. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAYON_TERRE_KM * Math.asin(Math.sqrt(a));
    }

    public static boolean coordonneesValides(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }
}
