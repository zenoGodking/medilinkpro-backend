package com.medilinkpro.backend.dto.request;

import com.medilinkpro.backend.enums.StatutRendezVous;
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
public class StatutRendezVousRequest {

    @NotNull(message = "Le nouveau statut est obligatoire")
    private StatutRendezVous statut;
}
