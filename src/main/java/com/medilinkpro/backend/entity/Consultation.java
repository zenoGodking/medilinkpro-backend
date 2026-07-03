package com.medilinkpro.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medilinkpro.backend.enums.TypeConsultation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consultation medicale : compte rendu, diagnostic, rattachee a un dossier medical,
 * a un medecin et a un patient. Peut donner lieu a une ordonnance.
 */
@Entity
@Table(name = "consultations")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    @ToString.Exclude
    private DossierMedical dossierMedical;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    @ToString.Exclude
    private Medecin medecin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @ToString.Exclude
    private Patient patient;

    @NotNull(message = "La date de consultation est obligatoire")
    @Column(name = "date_consultation", nullable = false)
    private LocalDateTime date;

    @Column(name = "motif", length = 255)
    private String motif;

    @Column(name = "diagnostic", columnDefinition = "TEXT")
    private String diagnostic;

    @Column(name = "compte_rendu", columnDefinition = "TEXT")
    private String compteRendu;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "type_consultation", length = 30)
    private TypeConsultation typeConsultation = TypeConsultation.PHYSIQUE;

    @JsonIgnore
    @OneToOne(mappedBy = "consultation", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private Ordonnance ordonnance;
}
