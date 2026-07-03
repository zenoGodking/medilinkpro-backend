package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedecinResponse {

    private UUID id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String specialite;
    private String numeroOrdre;
    private Double latitude;
    private Double longitude;
    private BigDecimal tarif;
    private boolean verifie;
    private UUID etablissementId;
    private String etablissementNom;
    private Double distanceApprox;
}
