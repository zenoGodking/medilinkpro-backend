package com.medilinkpro.backend.entity.suivi;

import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.TypeMesure;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mesure de suivi (tension, glycemie...) saisie par le patient (auto-mesure) ou par un medecin. */
@Entity
@Table(name = "mesures_sante", indexes = @Index(name = "idx_mesure_patient", columnList = "patient_id, type, date_mesure"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MesureSante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private TypeMesure type;

    @Column(name = "valeur", nullable = false)
    private Double valeur;

    /** Diastolique pour une tension ; null sinon. */
    @Column(name = "valeur2")
    private Double valeur2;

    /** Glycemie uniquement : mesure a jeun (les seuils d'interpretation different). */
    @Column(name = "a_jeun")
    private Boolean aJeun;

    @Column(name = "date_mesure", nullable = false)
    private LocalDateTime dateMesure;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "saisie_par_id", nullable = false)
    private UUID saisieParId;

    @Enumerated(EnumType.STRING)
    @Column(name = "saisie_par_role", length = 20, nullable = false)
    private Role saisieParRole;
}
