package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdonnanceRequest {

    @NotNull(message = "L'identifiant de la consultation est obligatoire")
    private UUID consultationId;

    private String medicaments;
    private String posologie;
}
