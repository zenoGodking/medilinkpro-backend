package com.medilinkpro.backend.entity;

import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.enums.TypeConsultation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rendez-vous pris par un patient auprès d'un medecin, physique ou en teleconsultation.
 */
@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@ToString(exclude = {"patient", "medecin", "consultation"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private EtablissementSante etablissement;

    @NotNull(message = "La date et l'heure du rendez-vous sont obligatoires")
    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 30)
    private StatutRendezVous statut = StatutRendezVous.EN_ATTENTE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private TypeConsultation type = TypeConsultation.PHYSIQUE;

    @Builder.Default
    @Column(name = "rappel_envoye", nullable = false)
    private boolean rappelEnvoye = false;

    @Column(name = "code_confirmation", length = 20)
    private String codeConfirmation;

    /** Rappel envoye une heure avant (le rappel de la veille utilise rappelEnvoye). */
    @Builder.Default
    @Column(name = "rappel_proche_envoye", nullable = false, columnDefinition = "boolean default false")
    private boolean rappelProcheEnvoye = false;

    /** Motif communique au patient par le medecin (refus, report, annulation). */
    @Column(name = "motif_medecin", length = 500)
    private String motifMedecin;

    /** Consultation (compte rendu, ordonnance) redigee a l'issue du rendez-vous, notamment d'une teleconsultation. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;

    /** Heure demandee a l'origine par le patient, si le medecin a reporte le rendez-vous. */
    @Column(name = "date_heure_initiale")
    private LocalDateTime dateHeureInitiale;

    @CreationTimestamp
    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;
}
