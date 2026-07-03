package com.medilinkpro.backend.dto.request;

import com.medilinkpro.backend.enums.TypeConsultation;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
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
public class RendezVousRequest {

    @NotNull(message = "L'identifiant du patient est obligatoire")
    private UUID patientId;

    @NotNull(message = "L'identifiant du medecin est obligatoire")
    private UUID medecinId;

    private UUID etablissementId;

    @NotNull(message = "La date et l'heure sont obligatoires")
    @Future(message = "La date du rendez-vous doit etre dans le futur")
    private LocalDateTime dateHeure;

    @Builder.Default
    private TypeConsultation type = TypeConsultation.PHYSIQUE;
}
