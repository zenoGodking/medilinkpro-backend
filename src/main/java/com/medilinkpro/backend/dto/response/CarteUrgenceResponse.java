package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.GroupeSanguin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Informations affichees quand on scanne la carte d'urgence d'un patient.
 * L'essentiel pour agir pour tout utilisateur connecte ; identite complete et acces
 * au carnet d'urgence pour le personnel de sante (champs omis sinon).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarteUrgenceResponse {

    private String prenom;
    private String photoReference;
    private GroupeSanguin groupeSanguin;
    private String allergies;
    private String conditionsUrgence;
    private String contactUrgenceNom;
    private String contactUrgenceTelephone;
    private boolean decede;

    // Personnel de sante uniquement
    private UUID patientId;
    private String nom;
    private LocalDate dateNaissance;
}
