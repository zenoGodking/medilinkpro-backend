package com.medilinkpro.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Etablissement de sante (hopital, clinique, cabinet) geolocalise.
 * Sert de support a la recherche et l'affichage sur carte interactive (Module Geolocalisation).
 */
@Entity
@Table(name = "etablissements", indexes = {
        @Index(name = "idx_etablissement_lat_lng", columnList = "latitude, longitude")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtablissementSante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Le nom de l'etablissement est obligatoire")
    @Column(name = "nom", nullable = false, length = 200)
    private String nom;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @ElementCollection
    @CollectionTable(name = "etablissement_specialites", joinColumns = @JoinColumn(name = "etablissement_id"))
    @Column(name = "specialite")
    @Builder.Default
    private List<String> specialitesDisponibles = new ArrayList<>();

    /**
     * URLs (relatives, servies via /uploads/**) des photos de l'etablissement.
     * L'ordre de la liste determine l'ordre d'affichage dans le carousel de la vitrine publique.
     */
    @ElementCollection
    @CollectionTable(name = "etablissement_photos", joinColumns = @JoinColumn(name = "etablissement_id"))
    @Column(name = "url", length = 500)
    @OrderColumn(name = "position")
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @OneToMany(mappedBy = "etablissement", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<Medecin> medecins = new ArrayList<>();
}
