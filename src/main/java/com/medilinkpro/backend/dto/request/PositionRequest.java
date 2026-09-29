package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Position GPS envoyee par l'application de l'infirmiere. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PositionRequest {

    @NotNull
    @DecimalMin("-90") @DecimalMax("90")
    private Double latitude;

    @NotNull
    @DecimalMin("-180") @DecimalMax("180")
    private Double longitude;
}
