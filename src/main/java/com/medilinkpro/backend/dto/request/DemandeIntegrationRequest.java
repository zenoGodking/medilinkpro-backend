package com.medilinkpro.backend.dto.request;

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
public class DemandeIntegrationRequest {

    /** Message d'accompagnement optionnel (ex: motivation, poste propose...). */
    private String message;
}
