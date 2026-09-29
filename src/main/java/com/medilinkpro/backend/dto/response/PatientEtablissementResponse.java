package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Patient ayant eu rendez-vous dans un etablissement du directeur : identite et activite
 * uniquement, aucune donnee medicale (reservee aux medecins).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEtablissementResponse {

    private UUID id;
    private String nom;
    private String prenom;
    private String telephone;
    private long nombreRendezVous;
    private LocalDateTime dernierRendezVous;
    private Set<String> etablissements;
}
