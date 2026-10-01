package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Creneau propose a la reservation ; libre = false s'il est deja pris par un autre rendez-vous. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreneauResponse {

    private LocalDateTime debut;
    private LocalDateTime fin;
    private boolean libre;
}
