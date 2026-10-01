package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.StatutCompte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Profil d'une infirmiere, presente au patient avant son arrivee (photo servie a part). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfirmierProfilResponse {

    private UUID id;
    private String nom;
    private String prenom;
    private String telephone;
    private boolean photoDisponible;

    private UUID etablissementId;
    private String etablissementNom;

    private Double noteMoyenne;
    private long nombreAvis;
    private long nombreInterventions;
    private LocalDateTime membreDepuis;

    /** Renseigne uniquement pour l'infirmiere elle-meme. */
    private StatutCompte statutCompte;
}
