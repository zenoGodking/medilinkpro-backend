package com.medilinkpro.backend.entity.suivi;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** Consultation prenatale (CPN) enregistree par un medecin. */
@Entity
@Table(name = "visites_prenatales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitePrenatale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grossesse_id", nullable = false)
    private Grossesse grossesse;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "poids")
    private Double poids;

    @Column(name = "tension_systolique")
    private Integer tensionSystolique;

    @Column(name = "tension_diastolique")
    private Integer tensionDiastolique;

    @Column(name = "hauteur_uterine_cm")
    private Double hauteurUterineCm;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "medecin_id", nullable = false)
    private UUID medecinId;

    @Column(name = "medecin_nom", length = 210, nullable = false)
    private String medecinNom;
}
