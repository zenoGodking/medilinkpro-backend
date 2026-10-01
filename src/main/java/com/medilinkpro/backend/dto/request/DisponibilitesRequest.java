package com.medilinkpro.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Semaine type du medecin : remplace l'ensemble de ses plages existantes. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilitesRequest {

    @NotNull
    @Size(max = 50, message = "50 plages au maximum")
    @Builder.Default
    private List<@Valid PlageDisponibiliteRequest> plages = new ArrayList<>();
}
