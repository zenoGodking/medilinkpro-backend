package com.medilinkpro.backend.securite;

import com.medilinkpro.backend.exception.TropDeTentativesException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de tentatives par cle (email, adresse IP...) sur une fenetre glissante,
 * pour freiner les attaques par force brute sur la connexion et la reinitialisation du mot de passe.
 * En memoire : suffisant pour une instance unique ; a deplacer dans un cache partage si l'API
 * est deployee sur plusieurs instances.
 */
@Component
public class LimiteurTentatives {

    private final Map<String, Deque<Long>> tentatives = new ConcurrentHashMap<>();
    private final Clock horloge;

    public LimiteurTentatives() {
        this(Clock.systemUTC());
    }

    LimiteurTentatives(Clock horloge) {
        this.horloge = horloge;
    }

    /** Leve TropDeTentativesException si la cle a deja atteint `maximum` tentatives sur la fenetre. */
    public void verifier(String cle, int maximum, Duration fenetre, String message) {
        Deque<Long> liste = tentatives.get(cle);
        if (liste == null) {
            return;
        }
        synchronized (liste) {
            purger(liste, fenetre);
            if (liste.size() >= maximum) {
                throw new TropDeTentativesException(message);
            }
        }
    }

    public void enregistrer(String cle, Duration fenetre) {
        Deque<Long> liste = tentatives.computeIfAbsent(cle, k -> new ArrayDeque<>());
        synchronized (liste) {
            purger(liste, fenetre);
            liste.addLast(horloge.millis());
        }
    }

    public void reinitialiser(String cle) {
        tentatives.remove(cle);
    }

    private void purger(Deque<Long> liste, Duration fenetre) {
        long limite = horloge.millis() - fenetre.toMillis();
        while (!liste.isEmpty() && liste.peekFirst() < limite) {
            liste.pollFirst();
        }
    }
}
