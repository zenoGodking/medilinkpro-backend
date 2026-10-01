package com.medilinkpro.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fichier (photo de page scannee, PDF) d'un document medical, stocke dans le dossier prive. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageDocument {

    @Column(name = "chemin", nullable = false, length = 255)
    private String chemin;

    @Column(name = "nom_original", length = 255)
    private String nomOriginal;

    @Column(name = "type_mime", length = 100)
    private String typeMime;

    @Column(name = "taille")
    private long taille;
}
