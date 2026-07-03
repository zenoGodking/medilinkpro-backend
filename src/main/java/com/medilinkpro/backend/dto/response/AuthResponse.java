package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String type;
    private UUID userId;
    private String email;
    private String nom;
    private String prenom;
    private Role role;

    /** Renseigne uniquement quand aucun token n'est emis (compte en attente de validation). */
    private String message;
}
