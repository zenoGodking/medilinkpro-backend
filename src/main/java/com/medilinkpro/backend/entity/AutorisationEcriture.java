package com.medilinkpro.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Autorisation donnee par un patient a un medecin d'ecrire dans son carnet (consultations,
 * ordonnances, analyses, donnees medicales). Tout medecin peut LIRE tous les carnets ;
 * l'ecriture exige cette autorisation, sauf pour un ancien patient du medecin
 * (voir CarnetAccesService). Revocable a tout moment par le patient.
 */
@Entity
@Table(name = "autorisations_ecriture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorisationEcriture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    @CreationTimestamp
    @Column(name = "date_autorisation", nullable = false, updatable = false)
    private LocalDateTime dateAutorisation;

    /** Null tant que l'autorisation est active. */
    @Column(name = "date_revocation")
    private LocalDateTime dateRevocation;
}
