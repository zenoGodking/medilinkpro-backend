package com.medilinkpro.backend.entity.suivi;

import com.medilinkpro.backend.entity.Patient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Suivi de grossesse : le terme et l'age gestationnel se calculent depuis la date des dernieres
 * regles (DDR). Les consultations prenatales sont saisies par un medecin autorise.
 */
@Entity
@Table(name = "grossesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grossesse {

    public enum Statut { EN_COURS, TERMINEE, INTERROMPUE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "date_dernieres_regles", nullable = false)
    private LocalDate dateDernieresRegles;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20, nullable = false)
    @Builder.Default
    private Statut statut = Statut.EN_COURS;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "issue", length = 500)
    private String issue;

    @OneToMany(mappedBy = "grossesse", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC")
    @Builder.Default
    private List<VisitePrenatale> visites = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
