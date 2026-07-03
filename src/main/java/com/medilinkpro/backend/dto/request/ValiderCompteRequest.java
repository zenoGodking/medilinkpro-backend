package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Decision de l'Admin sur un compte professionnel en attente : approbation ou refus
 * (avec motif obligatoire cote frontend en cas de refus).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValiderCompteRequest {

    @NotNull(message = "La decision (approuve) est obligatoire")
    private Boolean approuve;

    private String motifRejet;
}
