package com.medilinkpro.backend.entity.suivi;

import com.medilinkpro.backend.entity.Patient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Vaccin du carnet de vaccination. DECLAREE quand le patient l'ajoute lui-meme (ancien carnet papier),
 * VALIDEE quand un medecin l'enregistre ou confirme la declaration.
 */
@Entity
@Table(name = "vaccinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vaccination {

    public enum Statut { DECLAREE, VALIDEE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "vaccin", length = 150, nullable = false)
    private String vaccin;

    @Column(name = "dose", length = 60)
    private String dose;

    @Column(name = "date_vaccination", nullable = false)
    private LocalDate dateVaccination;

    @Column(name = "lot", length = 60)
    private String lot;

    @Column(name = "lieu", length = 200)
    private String lieu;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20, nullable = false)
    private Statut statut;

    @Column(name = "valide_par_id")
    private UUID valideParId;

    @Column(name = "valide_par_nom", length = 210)
    private String valideParNom;
}
