package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampagneResponse {

    private UUID id;
    private UUID etablissementId;
    private String etablissementNom;
    private String titre;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private boolean actif;
    /** Calcule cote serveur : actif=true ET dans la fenetre [dateDebut, dateFin]. */
    private boolean active;
    private LocalDateTime dateCreation;
}
