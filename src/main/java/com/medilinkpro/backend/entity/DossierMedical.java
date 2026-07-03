package com.medilinkpro.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medilinkpro.backend.enums.StatutDossier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dossier Medical Electronique (DME) : un par patient.
 * Centralise consultations, resultats d'analyses et ordonnances.
 */
@Entity
@Table(name = "dossiers_medicaux")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierMedical {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false, unique = true)
    @ToString.Exclude
    private Patient patient;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "derniere_mise_a_jour")
    private LocalDateTime derniereMiseAJour;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 30)
    private StatutDossier statut = StatutDossier.ACTIF;

    @Builder.Default
    @Column(name = "chiffrement_actif", nullable = false)
    private boolean chiffrementActif = true;

    @JsonIgnore
    @OneToMany(mappedBy = "dossierMedical", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Consultation> consultations = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "dossierMedical", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<ResultatAnalyse> resultatsAnalyses = new ArrayList<>();
}
