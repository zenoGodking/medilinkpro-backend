package com.medilinkpro.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** directeurId null = retirer le directeur de l'etablissement. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttributionDirecteurRequest {

    private UUID directeurId;
}
