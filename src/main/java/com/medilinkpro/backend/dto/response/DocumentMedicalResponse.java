package com.medilinkpro.backend.dto.response;

import com.medilinkpro.backend.enums.TypeDocumentMedical;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMedicalResponse {

    private UUID id;
    private UUID patientId;
    private TypeDocumentMedical type;
    private String titre;
    private String description;
    private LocalDate dateDocument;
    private LocalDateTime dateAjout;
    private UUID ajouteParId;
    private String ajouteParNom;
    /** true si l'utilisateur connecte peut supprimer ce document (le patient ou son auteur). */
    private boolean supprimable;
    private List<Page> pages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Page {
        private int index;
        private String nomOriginal;
        private String typeMime;
        private long taille;
    }
}
