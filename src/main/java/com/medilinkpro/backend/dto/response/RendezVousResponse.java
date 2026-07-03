package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.StatutRendezVous;
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
public class RendezVousResponse {

    private UUID id;
    private UUID patientId;
    private String patientNomComplet;
    private UUID medecinId;
    private String medecinNomComplet;
    private String specialiteMedecin;
    private UUID etablissementId;
    private String etablissementNom;
    private LocalDateTime dateHeure;
    private StatutRendezVous statut;
    private TypeConsultation type;
    private boolean rappelEnvoye;
    private String codeConfirmation;
    private LocalDateTime creeLe;
}
