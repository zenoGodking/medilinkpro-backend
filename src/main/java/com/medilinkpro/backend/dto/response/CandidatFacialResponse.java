package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.GroupeSanguin;
import com.medilinkpro.backend.enums.NiveauConfiance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Un candidat de correspondance faciale. Contient uniquement l'essentiel pour agir ;
 * nom et date de naissance ne sont renseignes que pour le personnel de sante
 * (ils sont omis du JSON sinon, cf. default-property-inclusion: non_null).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatFacialResponse {

    private int rang;
    private UUID patientId;

    /** Score indicatif de 0 a 100, derive de la distance entre empreintes. */
    private int scoreConfiance;
    private NiveauConfiance niveauConfiance;
    private double distance;

    /** Photo de reference enregistree a l'inscription (data URL), pour confirmation visuelle. */
    private String photoReference;

    private String prenom;
    private GroupeSanguin groupeSanguin;
    private String allergies;
    /** Asthme, diabete, epilepsie... : ce que les secours doivent savoir. */
    private String conditionsUrgence;
    private boolean decede;
    private String contactUrgenceNom;
    private String contactUrgenceTelephone;

    // Personnel de sante uniquement
    private String nom;
    private LocalDate dateNaissance;
}
