package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtablissementResponse {

    private UUID id;
    private String nom;
    private String type;
    private String adresse;
    private Double latitude;
    private Double longitude;
    private String telephone;
    private List<String> specialitesDisponibles;
    private List<String> photos;
}
