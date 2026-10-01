package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nouveau mot de passe genere par l'administrateur, affiche une seule fois pour qu'il le transmette. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotDePasseTemporaireResponse {
    private String nouveauMotDePasse;
}
