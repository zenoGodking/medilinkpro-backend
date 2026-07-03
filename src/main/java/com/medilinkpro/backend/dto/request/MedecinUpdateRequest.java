package com.medilinkpro.backend.dto.request;

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
public class MedecinUpdateRequest {

    private String nom;
    private String prenom;
    private String telephone;
    private String specialite;
    private String numeroOrdre;
    private Double latitude;
    private Double longitude;
    private BigDecimal tarif;
    private Boolean verifie;
    private UUID etablissementId;
}
