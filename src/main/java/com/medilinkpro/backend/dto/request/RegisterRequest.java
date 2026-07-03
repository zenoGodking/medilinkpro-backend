package com.medilinkpro.backend.dto.request;

import com.medilinkpro.backend.enums.GroupeSanguin;
import com.medilinkpro.backend.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Requete d'inscription. Le champ "role" determine le type concret cree
 * (PATIENT, MEDECIN, ADMIN, DIRECTEUR, SECRETAIRE).
 * Les champs specifiques au patient ou au medecin sont optionnels selon le role choisi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prenom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caracteres")
    private String motDePasse;

    @NotNull(message = "Le role est obligatoire")
    private Role role;

    private String telephone;

    // Champs specifiques PATIENT
    private LocalDate dateNaissance;
    private GroupeSanguin groupeSanguin;
    private String allergies;
    private String antecedents;
    private String numSecuriteSociale;

    // Champs specifiques MEDECIN
    private String specialite;
    private String numeroOrdre;
    private Double latitude;
    private Double longitude;
    private java.math.BigDecimal tarif;
}
