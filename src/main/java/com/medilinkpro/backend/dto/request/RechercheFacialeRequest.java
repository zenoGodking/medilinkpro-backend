package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Empreinte faciale (128 reels) calculee par le navigateur a partir de la photo scannee. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RechercheFacialeRequest {

    @NotNull(message = "L'empreinte faciale est obligatoire")
    private List<Double> descripteur;
}
