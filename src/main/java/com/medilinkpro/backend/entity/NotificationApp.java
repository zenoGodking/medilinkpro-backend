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
 * Notification affichee dans l'application (cloche). Toujours enregistree, meme si le push ou le SMS
 * ne peuvent pas etre delivres : l'utilisateur la retrouve a sa prochaine connexion.
 */
@Entity
@Table(name = "notifications_app", indexes = @Index(name = "idx_notif_destinataire", columnList = "destinataire_id, date_creation"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationApp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "destinataire_id", nullable = false)
    private UUID destinataireId;

    @Column(name = "titre", nullable = false, length = 150)
    private String titre;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    /** Page de l'application a ouvrir au clic (chemin interne). */
    @Column(name = "lien", length = 255)
    private String lien;

    @Builder.Default
    @Column(name = "lue", nullable = false)
    private boolean lue = false;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}
