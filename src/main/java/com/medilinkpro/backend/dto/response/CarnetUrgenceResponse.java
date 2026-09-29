package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Carnet medical complet d'un patient, consulte en lecture seule par le personnel de sante
 * a la suite d'une recherche par reconnaissance faciale.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarnetUrgenceResponse {

    private PatientResponse patient;
    private String photoReference;
    private List<ConsultationResponse> consultations;
    private List<ResultatAnalyseResponse> resultatsAnalyses;
    private List<OrdonnanceResponse> ordonnances;

    @Builder.Default
    private boolean lectureSeule = true;
}
