package com.medilinkpro.backend.dto.request;

import com.medilinkpro.backend.enums.TypeConsultation;
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
public class ConsultationRequest {

    @NotNull(message = "L'identifiant du patient est obligatoire")
    private UUID patientId;

    @NotNull(message = "L'identifiant du medecin est obligatoire")
    private UUID medecinId;

    @Builder.Default
    private LocalDateTime date = LocalDateTime.now();

    private String motif;
    private String diagnostic;
    private String compteRendu;

    @Builder.Default
    private TypeConsultation typeConsultation = TypeConsultation.PHYSIQUE;
}
