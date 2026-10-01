package com.medilinkpro.backend.dto.response;

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
public class ClotureTeleconsultationResponse {
    private ConsultationResponse consultation;
    /** null si aucune ordonnance n'a ete prescrite. */
    private OrdonnanceResponse ordonnance;
}
