package com.medilinkpro.backend.entity;

import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.TypeAccesCarnet;
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
 * Trace d'un acces aux donnees medicales d'un patient, consultable par le patient
 * ("qui a consulte mon carnet"). Contrepartie de la regle "tout medecin peut lire tous les carnets".
 */
@Entity
@Table(name = "acces_carnet", indexes = @Index(name = "idx_acces_carnet_patient", columnList = "patient_id, date_acces"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesCarnet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "utilisateur_role", length = 30, nullable = false)
    private Role utilisateurRole;

    /** Nom affiche au moment de l'acces (conserve meme si le compte est supprime ensuite). */
    @Column(name = "utilisateur_nom", length = 210, nullable = false)
    private String utilisateurNom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_acces", length = 30, nullable = false)
    private TypeAccesCarnet typeAcces;

    @CreationTimestamp
    @Column(name = "date_acces", nullable = false, updatable = false)
    private LocalDateTime dateAcces;
}
