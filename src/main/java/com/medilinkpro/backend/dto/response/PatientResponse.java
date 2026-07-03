package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.GroupeSanguin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private UUID id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateNaissance;
    private GroupeSanguin groupeSanguin;
    private String allergies;
    private String antecedents;
    private String numSecuriteSociale;
}
