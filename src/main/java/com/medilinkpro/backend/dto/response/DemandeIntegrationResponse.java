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

    /** MEDECIN ou INFIRMIER : le professionnel concerne par la demande. */
    private String typeProfessionnel;
    private UUID professionnelId;
    private String professionnelNom;
    private String professionnelPrenom;
    private String professionnelEmail;
    private String professionnelTelephone;

    private UUID infirmierId;
    private boolean infirmierPhotoDisponible;

    private UUID medecinId;
    private String medecinNom;
    private String medecinPrenom;
    private String medecinSpecialite;
    private String medecinNumeroOrdre;
    private String medecinEmail;
    private String medecinTelephone;

    private UUID etablissementId;
    private String etablissementNom;

    private InitiateurDemande initiateur;
    private StatutDemandeIntegration statut;
    private String message;
    private String messageReponse;

    private LocalDateTime dateCreation;
    private LocalDateTime dateReponse;
}
