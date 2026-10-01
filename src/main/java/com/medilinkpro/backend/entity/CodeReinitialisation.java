package com.medilinkpro.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Code a usage unique de reinitialisation du mot de passe, envoye par SMS (seul son hache est stocke). */
@Entity
@Table(name = "codes_reinitialisation", indexes = @Index(name = "idx_code_reinit_utilisateur", columnList = "utilisateur_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeReinitialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "code_hache", nullable = false, length = 100)
    private String codeHache;

    @Column(name = "expiration", nullable = false)
    private LocalDateTime expiration;

    @Builder.Default
    @Column(name = "tentatives", nullable = false)
    private int tentatives = 0;

    @Builder.Default
    @Column(name = "utilise", nullable = false)
    private boolean utilise = false;
}
