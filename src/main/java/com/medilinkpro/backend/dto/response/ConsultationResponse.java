package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.TypeConsultation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationResponse {

    private UUID id;
    private UUID dossierMedicalId;
    private UUID medecinId;
    private String medecinNomComplet;
    private UUID patientId;
    private String patientNomComplet;
    private LocalDateTime date;
    private String motif;
    private String diagnostic;
    private String compteRendu;
    private TypeConsultation typeConsultation;
    private UUID ordonnanceId;
}
