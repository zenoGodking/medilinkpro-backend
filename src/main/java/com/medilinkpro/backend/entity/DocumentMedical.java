package com.medilinkpro.backend.entity;

import com.medilinkpro.backend.securite.TexteChiffreConverter;
import jakarta.persistence.Convert;
import com.medilinkpro.backend.enums.TypeDocumentMedical;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Document medical ajoute au carnet par le patient (ou un medecin autorise) : antecedents, ancien
 * carnet papier scanne page par page, anciens resultats... Les fichiers sont stockes dans le dossier
 * prive et ne sont servis qu'apres verification des droits de lecture du carnet.
 */
@Entity
@Table(name = "documents_medicaux", indexes = {
        @Index(name = "idx_document_patient", columnList = "patient_id")
})
@Getter
@Setter
@ToString(exclude = {"patient", "ajoutePar", "pages"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMedical {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TypeDocumentMedical type;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Convert(converter = TexteChiffreConverter.class)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Date du document d'origine (ex: date de l'ancienne consultation), si connue. */
    @Column(name = "date_document")
    private LocalDate dateDocument;

    @ElementCollection
    @CollectionTable(name = "documents_medicaux_pages", joinColumns = @JoinColumn(name = "document_id"))
    @OrderColumn(name = "position")
    @Builder.Default
    private List<PageDocument> pages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ajoute_par_id")
    private Utilisateur ajoutePar;

    @CreationTimestamp
    @Column(name = "date_ajout", nullable = false, updatable = false)
    private LocalDateTime dateAjout;
}
