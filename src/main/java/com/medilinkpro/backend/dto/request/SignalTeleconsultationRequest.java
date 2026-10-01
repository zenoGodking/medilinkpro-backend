package com.medilinkpro.backend.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message de signalisation WebRTC echange entre le medecin et le patient pendant une
 * teleconsultation : join / ready / offer / answer / ice / leave / chat / media.
 * Le contenu (SDP, candidat ICE, texte) est relaye tel quel a l'autre participant.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalTeleconsultationRequest {

    @NotBlank
    private String type;

    private JsonNode data;
}
