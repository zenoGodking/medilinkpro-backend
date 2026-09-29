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
    /** Communique au patient une fois qu'une infirmiere a accepte la mission. */
    private String infirmierTelephone;

    /** Distance (km) entre l'infirmiere destinataire et le patient ; renseignee dans ses notifications. */
    private Double distanceKm;
    /** Nombre d'infirmieres a proximite deja notifiees (vagues successives). */
    private Integer nombreInfirmiersNotifies;
    private Boolean diffusionGenerale;

    private LocalDateTime dateCreation;
    private LocalDateTime dateReponse;

    private String compteRendu;
    private LocalDateTime dateCompteRendu;

    private Integer note;
    private String commentaire;
    private LocalDateTime dateNotation;
}
