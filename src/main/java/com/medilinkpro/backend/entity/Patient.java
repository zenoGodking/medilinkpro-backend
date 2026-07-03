package com.medilinkpro.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medilinkpro.backend.enums.GroupeSanguin;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Patient : utilisateur disposant d'un dossier medical et pouvant prendre des rendez-vous.
 */
@Entity
@Table(name = "patients")
@DiscriminatorValue("PATIENT")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Patient extends Utilisateur {

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    @Column(name = "groupe_sanguin", length = 20)
    private GroupeSanguin groupeSanguin;

    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "antecedents", columnDefinition = "TEXT")
    private String antecedents;

    @Column(name = "num_securite_sociale", length = 50)
    private String numSecuriteSociale;

    @JsonIgnore
    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private DossierMedical dossierMedical;

    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<RendezVous> rendezVousList = new ArrayList<>();
}
