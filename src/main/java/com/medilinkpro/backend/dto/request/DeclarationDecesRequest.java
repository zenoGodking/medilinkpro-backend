package com.medilinkpro.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeclarationDecesRequest {

    @NotNull(message = "La date du deces est obligatoire")
    @PastOrPresent(message = "La date du deces ne peut pas etre dans le futur")
    private LocalDate dateDeces;

    @Size(max = 2000)
    private String circonstances;
}
