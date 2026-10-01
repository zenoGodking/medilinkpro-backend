package com.medilinkpro.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.UUID;

/** Periode (conges, formation...) pendant laquelle le medecin ne recoit pas, bornes incluses. */
@Entity
@Table(name = "absences_medecin", indexes = {
        @Index(name = "idx_absence_medecin", columnList = "medecin_id")
})
@Getter
@Setter
@ToString(exclude = "medecin")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceMedecin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "motif", length = 255)
    private String motif;
}
