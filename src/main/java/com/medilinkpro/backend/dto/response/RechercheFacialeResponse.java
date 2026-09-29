package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.NiveauAccesUrgence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RechercheFacialeResponse {

    private NiveauAccesUrgence niveauAcces;

    /** Au plus 3 candidats, du plus ressemblant au moins ressemblant. Vide si aucune correspondance plausible. */
    private List<CandidatFacialResponse> candidats;

    /** Vrai si les deux meilleurs candidats sont trop proches pour etre departages. */
    private boolean ambigu;

    private String avertissement;
}
