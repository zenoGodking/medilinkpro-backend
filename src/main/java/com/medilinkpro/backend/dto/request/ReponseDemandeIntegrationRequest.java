package com.medilinkpro.backend.dto.request;

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
public class ReponseDemandeIntegrationRequest {

    @NotNull(message = "La decision (accepter) est obligatoire")
    private Boolean accepter;

    private String messageReponse;
}
