package com.medilinkpro.backend.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
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
public class SignalTeleconsultationResponse {

    private UUID rendezVousId;
    private UUID de;
    private String deRole;
    private String type;
    private JsonNode data;
}
