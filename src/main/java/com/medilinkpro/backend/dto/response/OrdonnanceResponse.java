package com.medilinkpro.backend.dto.response;

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
public class OrdonnanceResponse {

    private UUID id;
    private UUID consultationId;
    private UUID medecinId;
    private String medecinNomComplet;
    private UUID patientId;
    private String patientNomComplet;
    private LocalDateTime dateEmission;
    private String medicaments;
    private String posologie;
    private String signatureElectronique;
    private String codeQr;
}
