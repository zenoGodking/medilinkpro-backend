package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoterAlerteRequest {

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note doit etre comprise entre 1 et 5")
    @Max(value = 5, message = "La note doit etre comprise entre 1 et 5")
    private Integer note;

    private String commentaire;
}
