package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resume d'un compte professionnel (Medecin, Infirmier, Directeur) destine
 * a l'ecran de validation Admin. Les champs specialite/numeroOrdre ne sont
 * renseignes que pour un Medecin.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteEnAttenteResponse {

    private UUID id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private Role role;
    private StatutCompte statutCompte;
    private boolean actif;
    private String motifRejet;
    private LocalDateTime dateInscription;

    // Specifique Medecin
    private String specialite;
    private String numeroOrdre;

    // Specifique Infirmier : photo de profil fournie (servie par /api/infirmiers/{id}/photo)
    private Boolean photoDisponible;
}
