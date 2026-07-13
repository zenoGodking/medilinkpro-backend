package com.medilinkpro.backend.entity;

import com.medilinkpro.backend.enums.StatutAlerte;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
 * Alerte envoyee par un patient a toutes les infirmieres connectees pour demander
 * un soin a domicile. Diffusee en temps reel via WebSocket (voir WebSocketConfig /
 * AlerteService) ; la premiere infirmiere qui repond devient responsable de
 * l'intervention et l'alerte n'est plus proposee aux autres.
 */
@Entity
@Table(name = "alertes_soin_domicile")
@Getter
@Setter
@ToString(exclude = {"patient", "infirmier"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteSoinDomicile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotBlank(message = "L'adresse d'intervention est obligatoire")
    @Column(name = "adresse", nullable = false, length = 500)
    private String adresse;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "message", length = 1000)
    private String message;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAlerte statut = StatutAlerte.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "infirmier_id")
    private Infirmier infirmier;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    /** Compte-rendu redige par l'infirmiere a la fin de son intervention (obligatoire
     * pour liberer l'infirmiere et lui permettre de repondre a une nouvelle alerte). */
    @Column(name = "compte_rendu", columnDefinition = "TEXT")
    private String compteRendu;

    @Column(name = "date_compte_rendu")
    private LocalDateTime dateCompteRendu;

    /** Note laissee par le patient a la fin du service rendu (1 a 5), et commentaire libre. */
    @Column(name = "note")
    private Integer note;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @Column(name = "date_notation")
    private LocalDateTime dateNotation;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
