package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ce que voit le pharmacien en scannant une ordonnance : son contenu, de quoi verifier
 * l'identite du patient et du prescripteur, et son etat (validite, delivrance). Rien d'autre du carnet.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationOrdonnanceResponse {

    private String medicaments;
    private String posologie;
    private LocalDateTime dateEmission;
    private LocalDate dateExpiration;
    private boolean expiree;

    private boolean delivree;
    private LocalDateTime dateDelivrance;
    private String delivreePar;

    private String patientNom;
    private String patientPrenom;
    private LocalDate patientDateNaissance;

    private String medecinNomComplet;
    private String medecinSpecialite;
    private String medecinNumeroOrdre;
    private String signatureElectronique;
}
