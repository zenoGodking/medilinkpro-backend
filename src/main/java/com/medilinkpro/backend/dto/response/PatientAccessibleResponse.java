package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.MotifEcriture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Patient dans le carnet duquel le medecin connecte peut ecrire. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAccessibleResponse {

    private UUID id;
    private String nom;
    private String prenom;
    private MotifEcriture motif;
}
