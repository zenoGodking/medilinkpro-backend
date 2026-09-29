package com.medilinkpro.backend.dto.response;

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
public class AutorisationResponse {

    private UUID medecinId;
    private String medecinNom;
    private String medecinPrenom;
    private String specialite;
    private LocalDateTime dateAutorisation;
}
