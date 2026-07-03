package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatAnalyseResponse {

    private UUID id;
    private UUID dossierMedicalId;
    private String type;
    private LocalDate dateResultat;
    private String fichierUrl;
    private String statut;
    private String laboratoire;
}
