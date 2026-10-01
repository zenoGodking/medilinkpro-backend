package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtablissementRequest {

    @NotBlank(message = "Le nom de l'etablissement est obligatoire")
    private String nom;

    private String type;
    private String adresse;
    private String ville;
    private String quartier;
    private String telephone;

    @jakarta.validation.constraints.DecimalMin(value = "-90.0", message = "Latitude invalide")
    @jakarta.validation.constraints.DecimalMax(value = "90.0", message = "Latitude invalide")
    private Double latitude;

    @jakarta.validation.constraints.DecimalMin(value = "-180.0", message = "Longitude invalide")
    @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "Longitude invalide")
    private Double longitude;
    private List<String> specialitesDisponibles;
}
