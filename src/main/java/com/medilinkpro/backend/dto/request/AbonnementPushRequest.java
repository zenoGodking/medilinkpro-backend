package com.medilinkpro.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Format de PushSubscription.toJSON() cote navigateur. */
public record AbonnementPushRequest(
        @NotBlank @Pattern(regexp = "^https://.+", message = "Endpoint push invalide") String endpoint,
        @NotNull @Valid Cles keys) {

    public record Cles(@NotBlank String p256dh, @NotBlank String auth) {
    }
}
