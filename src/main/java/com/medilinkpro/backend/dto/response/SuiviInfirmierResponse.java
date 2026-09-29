package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Position en temps reel de l'infirmiere en route vers le patient. Poussee au patient sur
 * "/user/queue/suivi" a chaque deplacement, uniquement pendant l'intervention (statut REPONDUE).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuiviInfirmierResponse {

    private UUID alerteId;
    private UUID infirmierId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime datePosition;
    /** Distance restante a vol d'oiseau jusqu'au patient, si sa position est connue. */
    private Double distanceKm;
}
