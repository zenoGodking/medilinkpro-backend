package com.medilinkpro.backend.dto.request;

import com.medilinkpro.backend.enums.TypeDocumentMedical;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMedicalRequest {

    @NotNull(message = "Le type de document est obligatoire")
    private TypeDocumentMedical type;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200)
    private String titre;

    @Size(max = 4000)
    private String description;

    private LocalDate dateDocument;
}
