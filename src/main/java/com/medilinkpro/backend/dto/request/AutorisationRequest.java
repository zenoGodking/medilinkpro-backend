package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutorisationRequest {

    @NotNull(message = "Le medecin est obligatoire")
    private UUID medecinId;
}
