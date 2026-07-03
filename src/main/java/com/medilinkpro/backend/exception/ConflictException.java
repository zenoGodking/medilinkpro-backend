package com.medilinkpro.backend.exception;

/**
 * Levee lors d'un conflit d'etat, ex : une infirmiere tente de repondre a une
 * alerte deja prise en charge par une autre.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
