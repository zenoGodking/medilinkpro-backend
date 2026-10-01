package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Decision du medecin sur un rendez-vous : motif (refus, annulation) ou nouvelle heure (report). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRendezVousRequest {

    @Size(max = 500)
    private String motif;

    /** Obligatoire pour un report : debut d'un creneau libre du calendrier du medecin. */
    private LocalDateTime nouvelleDateHeure;
}
