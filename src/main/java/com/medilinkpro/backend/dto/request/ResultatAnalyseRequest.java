package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class ResultatAnalyseRequest {

    @NotNull(message = "L'identifiant du dossier medical est obligatoire")
    private UUID dossierMedicalId;

    private String type;
    private LocalDate dateResultat;
    private String fichierUrl;
    private String statut;
    private String laboratoire;
}
