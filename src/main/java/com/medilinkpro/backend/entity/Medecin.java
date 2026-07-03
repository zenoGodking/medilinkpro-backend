package com.medilinkpro.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Medecin / Specialiste : professionnel de sante proposant consultations et teleconsultations.
 * Geolocalise (lat/lng) pour la recherche de specialistes a proximite.
 */
@Entity
@Table(name = "medecins")
@DiscriminatorValue("MEDECIN")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Medecin extends Utilisateur {

    @Column(name = "specialite", length = 100)
    private String specialite;

    @Column(name = "numero_ordre", length = 50)
    private String numeroOrdre;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "tarif", precision = 10, scale = 2)
    private BigDecimal tarif;

    @Builder.Default
    @Column(name = "verifie", nullable = false)
    private boolean verifie = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private EtablissementSante etablissement;

    @JsonIgnore
    @OneToMany(mappedBy = "medecin", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<RendezVous> rendezVousList = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "medecin", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<Consultation> consultations = new ArrayList<>();
}
