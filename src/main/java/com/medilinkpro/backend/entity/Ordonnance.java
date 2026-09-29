package com.medilinkpro.backend.entity;

import jakarta.persistence.*;
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
 * Ordonnance numerique generee a l'issue d'une consultation,
 * verifiable en pharmacie via code QR.
 */
@Entity
@Table(name = "ordonnances")
@Getter
@Setter
@ToString(exclude = {"consultation", "medecin", "patient"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ordonnance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false, unique = true)
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @CreationTimestamp
    @Column(name = "date_emission", nullable = false, updatable = false)
    private LocalDateTime dateEmission;

    @Column(name = "medicaments", columnDefinition = "TEXT")
    private String medicaments;

    @Column(name = "posologie", columnDefinition = "TEXT")
    private String posologie;

    @Column(name = "signature_electronique", length = 255)
    private String signatureElectronique;

    @Column(name = "code_qr", length = 255)
    private String codeQr;

    /**
     * Jeton aleatoire (192 bits) encode dans le QR code montre en pharmacie. Genere a la demande
     * pour les ordonnances anterieures a cette fonctionnalite.
     */
    @Column(name = "jeton_verification", length = 64, unique = true)
    private String jetonVerification;

    @Column(name = "date_delivrance")
    private LocalDateTime dateDelivrance;

    @Column(name = "pharmacien_id")
    private java.util.UUID pharmacienId;

    /** Pharmacie et pharmacien au moment de la delivrance (conserves meme si le compte disparait). */
    @Column(name = "delivree_par", length = 300)
    private String delivreePar;
}
