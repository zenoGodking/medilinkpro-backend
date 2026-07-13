package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.InitiateurDemande;
import com.medilinkpro.backend.enums.StatutDemandeIntegration;
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
public class DemandeIntegrationResponse {

    private UUID id;

    private UUID medecinId;
    private String medecinNom;
    private String medecinPrenom;
    private String medecinSpecialite;

    private UUID etablissementId;
    private String etablissementNom;

    private InitiateurDemande initiateur;
    private StatutDemandeIntegration statut;
    private String message;
    private String messageReponse;

    private LocalDateTime dateCreation;
    private LocalDateTime dateReponse;
}
