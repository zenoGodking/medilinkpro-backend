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

/** Trace de chaque SMS emis par l'application (ex: information des proches d'un deces). */
@Entity
@Table(name = "notifications_sms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSms {

    public enum Statut { ENVOYE, NON_ENVOYE_AUCUN_FOURNISSEUR, ECHEC }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "destinataire", length = 30, nullable = false)
    private String destinataire;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "contexte", length = 50)
    private String contexte;

    @Column(name = "patient_id")
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 40, nullable = false)
    private Statut statut;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
