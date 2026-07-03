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
    private Double latitude;
    private Double longitude;
    private String telephone;
    private List<String> specialitesDisponibles;
}
