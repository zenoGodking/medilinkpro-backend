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
@ToString(exclude = {"patient", "medecin"})
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

    @CreationTimestamp
    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;
}
