package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.MotifEcriture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Carnet medical complet d'un patient, avec les droits d'ecriture du medecin qui le consulte. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarnetResponse {

    private PatientResponse patient;
    private List<ConsultationResponse> consultations;
    private List<ResultatAnalyseResponse> resultatsAnalyses;
    private List<OrdonnanceResponse> ordonnances;

    /** Vrai si l'utilisateur connecte (medecin) peut ecrire dans ce carnet. */
    private boolean ecritureAutorisee;
    private MotifEcriture motifEcriture;
}
