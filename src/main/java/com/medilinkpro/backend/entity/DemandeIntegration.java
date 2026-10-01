package com.medilinkpro.backend.entity;

import com.medilinkpro.backend.enums.InitiateurDemande;
import com.medilinkpro.backend.enums.StatutDemandeIntegration;
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
 * Demande d'integration d'un professionnel dans un etablissement de sante : soit
 * l'etablissement invite un medecin (celui-ci doit valider), soit un medecin ou une
 * infirmiere demande a rejoindre l'etablissement (le Directeur/Admin doit valider).
 * Exactement un des deux champs medecin / infirmier est renseigne.
 * Une fois acceptee, Medecin.etablissement ou Infirmier.etablissement est mis a jour.
 */
@Entity
@Table(name = "demandes_integration")
@Getter
@Setter
@ToString(exclude = {"medecin", "infirmier", "etablissement"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id")
    private Medecin medecin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "infirmier_id")
    private Infirmier infirmier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id", nullable = false)
    private EtablissementSante etablissement;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiateur", nullable = false, length = 20)
    private InitiateurDemande initiateur;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutDemandeIntegration statut = StatutDemandeIntegration.EN_ATTENTE;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "message_reponse", length = 1000)
    private String messageReponse;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;
}
