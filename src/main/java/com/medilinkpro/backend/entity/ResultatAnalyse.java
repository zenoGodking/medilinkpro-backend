package com.medilinkpro.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Resultat d'analyse biologique ou d'imagerie, importe depuis un laboratoire
 * partenaire et rattache au dossier medical du patient.
 */
@Entity
@Table(name = "resultats_analyses")
@Getter
@Setter
@ToString(exclude = "dossierMedical")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatAnalyse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierMedical dossierMedical;

    @Column(name = "type_analyse", length = 100)
    private String type;

    @Column(name = "date_resultat")
    private LocalDate dateResultat;

    @Column(name = "fichier_url", length = 500)
    private String fichierUrl;

    @Column(name = "statut", length = 30)
    private String statut;

    @Column(name = "laboratoire", length = 150)
    private String laboratoire;
}
