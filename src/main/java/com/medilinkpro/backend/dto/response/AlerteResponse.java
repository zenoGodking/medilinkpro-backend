package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.StatutAlerte;
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
public class AlerteResponse {

    private UUID id;

    private UUID patientId;
    private String patientNom;
    private String patientPrenom;
    private String patientTelephone;

    private String adresse;
    private Double latitude;
    private Double longitude;
    private String message;

    private StatutAlerte statut;

    private UUID infirmierId;
    private String infirmierNom;
    private String infirmierPrenom;

    private LocalDateTime dateCreation;
    private LocalDateTime dateReponse;

    private Integer note;
    private String commentaire;
    private LocalDateTime dateNotation;
}
