package com.medilinkpro.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilitesResponse {

    private UUID medecinId;

    /** true si le medecin n'a pas encore defini sa semaine : les heures ouvrables par defaut s'appliquent. */
    private boolean parDefaut;

    private List<Plage> plages;
    private List<Absence> absences;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Plage {
        private DayOfWeek jourSemaine;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime heureDebut;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime heureFin;
        private int dureeCreneauMinutes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Absence {
        private UUID id;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private String motif;
    }
}
