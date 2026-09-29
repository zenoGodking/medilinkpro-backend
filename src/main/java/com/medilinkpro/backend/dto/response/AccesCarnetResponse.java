package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.TypeAccesCarnet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesCarnetResponse {

    private LocalDateTime dateAcces;
    private TypeAccesCarnet typeAcces;
    private Role role;
    /** Nom du professionnel ; masque (null) quand l'acces vient d'un autre patient. */
    private String nom;
}
