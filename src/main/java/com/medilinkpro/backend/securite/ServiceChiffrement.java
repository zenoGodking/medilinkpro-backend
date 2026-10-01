package com.medilinkpro.backend.securite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Chiffrement applicatif AES-256-GCM des donnees de sante au repos : champs sensibles en base
 * (via TexteChiffreConverter) et fichiers prives (photos, documents scannes, via FileStorageService).
 * La cle (medilinkpro.chiffrement.cle, 32 octets en Base64) ne doit jamais etre perdue ni changee
 * sans re-chiffrement : sans elle, les donnees sont illisibles.
 * Les valeurs anterieures au chiffrement (sans prefixe/en-tete) sont relues telles quelles.
 */
@Component
public class ServiceChiffrement {

    public static final String PREFIXE_TEXTE = "enc:v1:";
    private static final byte[] ENTETE_FICHIER = "MLPENC1".getBytes(StandardCharsets.US_ASCII);
    private static final int TAILLE_IV = 12;
    private static final int TAILLE_TAG_BITS = 128;
    private static final SecureRandom ALEATOIRE = new SecureRandom();

    /** Accessible aux convertisseurs JPA, instancies par Hibernate. */
    private static volatile ServiceChiffrement instance;

    private final SecretKeySpec cle;

    public ServiceChiffrement(@Value("${medilinkpro.chiffrement.cle:}") String cleBase64) {
        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(cleBase64);
        } catch (IllegalArgumentException e) {
            octets = new byte[0];
        }
        if (octets.length != 32) {
            throw new IllegalStateException(
                    "CLE_CHIFFREMENT doit être une clé AES-256 de 32 octets encodée en Base64 (openssl rand -base64 32)");
        }
        this.cle = new SecretKeySpec(octets, "AES");
        instance = this;
    }

    public static ServiceChiffrement instance() {
        if (instance == null) {
            throw new IllegalStateException("Service de chiffrement non initialisé");
        }
        return instance;
    }

    // ------------------------------------------------------------------ Textes

    public String chiffrerTexte(String clair) {
        if (clair == null || clair.startsWith(PREFIXE_TEXTE)) {
            return clair;
        }
        return PREFIXE_TEXTE + Base64.getEncoder().encodeToString(chiffrer(clair.getBytes(StandardCharsets.UTF_8)));
    }

    public String dechiffrerTexte(String valeur) {
        if (valeur == null || !valeur.startsWith(PREFIXE_TEXTE)) {
            return valeur; // donnee anterieure au chiffrement
        }
        byte[] donnees = Base64.getDecoder().decode(valeur.substring(PREFIXE_TEXTE.length()));
        return new String(dechiffrer(donnees), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ Fichiers

    public byte[] chiffrerFichier(byte[] clair) {
        if (estFichierChiffre(clair)) {
            return clair;
        }
        byte[] chiffre = chiffrer(clair);
        return ByteBuffer.allocate(ENTETE_FICHIER.length + chiffre.length).put(ENTETE_FICHIER).put(chiffre).array();
    }

    public byte[] dechiffrerFichier(byte[] contenu) {
        if (!estFichierChiffre(contenu)) {
            return contenu; // fichier anterieur au chiffrement
        }
        return dechiffrer(Arrays.copyOfRange(contenu, ENTETE_FICHIER.length, contenu.length));
    }

    public static boolean estFichierChiffre(byte[] contenu) {
        return contenu.length >= ENTETE_FICHIER.length
                && Arrays.equals(Arrays.copyOf(contenu, ENTETE_FICHIER.length), ENTETE_FICHIER);
    }

    // ------------------------------------------------------------------ AES-GCM

    private byte[] chiffrer(byte[] clair) {
        try {
            byte[] iv = new byte[TAILLE_IV];
            ALEATOIRE.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG_BITS, iv));
            byte[] chiffre = cipher.doFinal(clair);
            return ByteBuffer.allocate(iv.length + chiffre.length).put(iv).put(chiffre).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Échec du chiffrement", e);
        }
    }

    private byte[] dechiffrer(byte[] donnees) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG_BITS, donnees, 0, TAILLE_IV));
            return cipher.doFinal(donnees, TAILLE_IV, donnees.length - TAILLE_IV);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Donnée chiffrée illisible : clé de chiffrement incorrecte ou donnée altérée", e);
        }
    }
}
