package com.medilinkpro.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Campagne (vaccination, depistage, promotion...) lancee par un etablissement de
 * sante, affichee publiquement (vitrine, fiche de l'etablissement) sans que le
 * visiteur ait besoin de s'inscrire.
 */
@Entity
@Table(name = "campagnes")
@Getter
@Setter
@ToString(exclude = "etablissement")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campagne {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id", nullable = false)
    private EtablissementSante etablissement;

    @NotBlank(message = "Le titre de la campagne est obligatoire")
    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "description", length = 2000)
    private String description;

    @NotNull(message = "La date de debut est obligatoire")
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /** Null = campagne sans date de fin definie (active tant qu'elle n'est pas desactivee manuellement). */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
