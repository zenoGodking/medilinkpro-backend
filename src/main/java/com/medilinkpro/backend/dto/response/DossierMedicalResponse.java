package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.StatutDossier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierMedicalResponse {

    private UUID id;
    private UUID patientId;
    private String patientNomComplet;
    private LocalDateTime dateCreation;
    private LocalDateTime derniereMiseAJour;
    private StatutDossier statut;
    private boolean chiffrementActif;
    private List<ConsultationResponse> consultations;
    private List<ResultatAnalyseResponse> resultatsAnalyses;
}
