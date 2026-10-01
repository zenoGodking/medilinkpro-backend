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
 * Avis d'un patient sur un medecin, laisse apres un rendez-vous effectue (un avis par rendez-vous).
 * L'administrateur peut masquer un avis abusif sans le supprimer.
 */
@Entity
@Table(name = "avis_medecins", indexes = @Index(name = "idx_avis_medecin", columnList = "medecin_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvisMedecin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rendez_vous_id", nullable = false, unique = true)
    private RendezVous rendezVous;

    @Column(name = "note", nullable = false)
    private int note;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @Builder.Default
    @Column(name = "masque", nullable = false)
    private boolean masque = false;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
