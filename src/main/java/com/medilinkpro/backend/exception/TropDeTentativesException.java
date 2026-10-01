package com.medilinkpro.backend.exception;

/** Trop de tentatives (connexion, code de reinitialisation) : HTTP 429. */
public class TropDeTentativesException extends RuntimeException {
    public TropDeTentativesException(String message) {
        super(message);
    }
}
