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
 * Journal d'audit des acces aux donnees medicales via la reconnaissance faciale :
 * qui a consulte quoi, pour quel patient et quand. Permet de detecter un usage abusif
 * d'une fonctionnalite ouverte a tous les utilisateurs connectes.
 */
@Entity
@Table(name = "acces_urgence_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesUrgenceLog {

    public enum TypeAcces { RECHERCHE_FACIALE, CARNET_COMPLET }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    @Column(name = "utilisateur_role", length = 30, nullable = false)
    private String utilisateurRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_acces", length = 30, nullable = false)
    private TypeAcces typeAcces;

    /** Patient consulte (carnet) ou meilleur candidat (recherche) ; null si aucune correspondance. */
    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "nombre_candidats")
    private Integer nombreCandidats;

    @Column(name = "meilleure_distance")
    private Double meilleureDistance;

    @CreationTimestamp
    @Column(name = "date_acces", nullable = false, updatable = false)
    private LocalDateTime dateAcces;
}
