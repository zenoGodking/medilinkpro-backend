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
    private String ville;
    private String quartier;
    private String telephone;
    private List<String> specialitesDisponibles;
    private List<String> photos;
    private long nombreVisites;
    private java.util.UUID directeurId;
    private String directeurNomComplet;
}
