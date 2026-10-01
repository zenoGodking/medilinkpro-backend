package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvisResponse {
    private UUID id;
    private UUID medecinId;
    private int note;
    private String commentaire;
    /** Prenom et initiale du nom uniquement (ex : "Aline M.") pour preserver la vie privee du patient. */
    private String auteur;
    private LocalDateTime dateCreation;
    /** Renseigne pour l'administrateur (moderation). */
    private Boolean masque;
}
