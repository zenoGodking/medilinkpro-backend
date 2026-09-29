package com.medilinkpro.backend.entity.suivi;

import com.medilinkpro.backend.entity.Patient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Rappel de prise de medicament gere par le patient (notification : voir les notifications push). */
@Entity
@Table(name = "rappels_medicament")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RappelMedicament {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "medicament", length = 200, nullable = false)
    private String medicament;

    @Column(name = "dosage", length = 200)
    private String dosage;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rappel_medicament_heures", joinColumns = @JoinColumn(name = "rappel_id"))
    @Column(name = "heure")
    @OrderBy
    @Builder.Default
    private List<LocalTime> heures = new ArrayList<>();

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /** Null = traitement au long cours. */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /** Derniere prise notifiee ("2026-09-29T08:00") : evite d'envoyer deux fois le meme rappel. */
    @Column(name = "dernier_envoi", length = 20)
    private String dernierEnvoi;
}
