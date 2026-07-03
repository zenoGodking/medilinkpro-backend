package com.medilinkpro.backend.exception;

/**
 * Levee lorsqu'un utilisateur dont le compte est EN_ATTENTE ou REJETE tente de se connecter.
 */
public class CompteNonValideException extends RuntimeException {
    public CompteNonValideException(String message) {
        super(message);
    }
}
