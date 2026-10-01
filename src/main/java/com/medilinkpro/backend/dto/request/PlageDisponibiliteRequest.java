package com.medilinkpro.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlageDisponibiliteRequest {

    @NotNull(message = "Le jour est obligatoire")
    private DayOfWeek jourSemaine;

    @NotNull(message = "L'heure de debut est obligatoire")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureDebut;

    @NotNull(message = "L'heure de fin est obligatoire")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureFin;

    @Min(value = 10, message = "Un creneau dure au moins 10 minutes")
    @Max(value = 240, message = "Un creneau dure au plus 4 heures")
    @Builder.Default
    private int dureeCreneauMinutes = 30;
}
