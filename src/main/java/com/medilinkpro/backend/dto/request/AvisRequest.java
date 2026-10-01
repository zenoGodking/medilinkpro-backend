package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvisRequest {

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note va de 1 a 5")
    @Max(value = 5, message = "La note va de 1 a 5")
    private Integer note;

    @Size(max = 1000)
    private String commentaire;
}
