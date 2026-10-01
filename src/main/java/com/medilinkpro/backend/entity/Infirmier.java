package com.medilinkpro.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Infirmier(e) : intervient a domicile en reponse aux alertes de soins envoyees
 * par les patients (voir AlerteSoinDomicile). Compte soumis a validation Admin.
 */
@Entity
@Table(name = "infirmiers")
@DiscriminatorValue("INFIRMIER")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Infirmier extends Utilisateur {

    /**
     * Derniere position GPS connue, envoyee en continu par l'application tant que
     * l'infirmiere a sa page d'alertes ouverte (voir PositionInfirmierController).
     * Sert a notifier les infirmieres les plus proches d'une alerte, puis au suivi
     * en temps reel par le patient pendant l'intervention.
     */
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "date_position")
    private LocalDateTime datePosition;

    /**
     * Photo de profil (dossier prive, chemin relatif) : obligatoire pour repondre aux alertes,
     * afin que le patient sache qui va venir chez lui. Servie par InfirmierController.
     */
    @Column(name = "photo_profil_chemin", length = 255)
    private String photoProfilChemin;

    /** Etablissement de rattachement, apres acceptation d'une demande d'integration. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    @ToString.Exclude
    private EtablissementSante etablissement;
}
