package com.medilinkpro.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Plage horaire hebdomadaire pendant laquelle un medecin recoit (ex: lundi 08:00-12:00, creneaux de 30 min).
 * Les creneaux proposes aux patients sont generes a partir de ces plages (voir DisponibiliteService).
 */
@Entity
@Table(name = "plages_disponibilite", indexes = {
        @Index(name = "idx_plage_medecin", columnList = "medecin_id")
})
@Getter
@Setter
@ToString(exclude = "medecin")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlageDisponibilite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    @Enumerated(EnumType.STRING)
    @Column(name = "jour_semaine", nullable = false, length = 10)
    private DayOfWeek jourSemaine;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(name = "duree_creneau_minutes", nullable = false)
    private int dureeCreneauMinutes;
}
