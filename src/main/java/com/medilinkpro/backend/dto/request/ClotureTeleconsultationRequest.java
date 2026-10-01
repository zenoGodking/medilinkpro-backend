package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fin de teleconsultation : compte rendu (obligatoire) et ordonnance (facultative). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClotureTeleconsultationRequest {

    @Size(max = 255)
    private String motif;

    @Size(max = 4000)
    private String diagnostic;

    @NotBlank(message = "Le compte rendu est obligatoire")
    @Size(max = 8000)
    private String compteRendu;

    /** Si renseigne, une ordonnance numerique (verifiable en pharmacie) est emise. */
    @Size(max = 4000)
    private String medicaments;

    @Size(max = 4000)
    private String posologie;
}
