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

/** Abonnement aux notifications push d'un appareil (navigateur ou application installee). */
@Entity
@Table(name = "abonnements_push", indexes = @Index(name = "idx_abonnement_utilisateur", columnList = "utilisateur_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonnementPush {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    @Column(name = "endpoint", length = 1000, nullable = false, unique = true)
    private String endpoint;

    @Column(name = "p256dh", length = 200, nullable = false)
    private String p256dh;

    @Column(name = "auth", length = 100, nullable = false)
    private String auth;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
