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

    /**
     * Conditions a signaler aux secours (asthme, diabete, epilepsie, pacemaker...), renseignees
     * par le patient en sachant qu'elles sont visibles de tout utilisateur qui le retrouve
     * accidente (contrairement aux antecedents, reserves au personnel de sante).
     */
    @Column(name = "conditions_urgence", columnDefinition = "TEXT")
    private String conditionsUrgence;

    /**
     * Jeton aleatoire (192 bits) encode dans le QR code de la carte d'urgence. Il ne contient
     * aucune donnee : il permet seulement, a un utilisateur connecte, d'afficher les informations
     * d'urgence. Le patient peut le regenerer pour invalider une carte perdue.
     */
    @JsonIgnore
    @Column(name = "jeton_carte_urgence", length = 64, unique = true)
    private String jetonCarteUrgence;

    /** Deces declare par un medecin : le compte est desactive et le proche est informe. */
    @Builder.Default
    @Column(name = "decede", nullable = false)
    private boolean decede = false;

    @Column(name = "date_deces")
    private LocalDate dateDeces;

    @Column(name = "circonstances_deces", columnDefinition = "TEXT")
    private String circonstancesDeces;

    @Column(name = "deces_declare_par")
    private java.util.UUID decesDeclarePar;

    @Column(name = "date_declaration_deces")
    private java.time.LocalDateTime dateDeclarationDeces;

    @Column(name = "num_securite_sociale", length = 50)
    private String numSecuriteSociale;

    /** Contact d'un proche a joindre en cas d'urgence, renseigne a l'inscription. */
    @Column(name = "contact_urgence_nom", length = 150)
    private String contactUrgenceNom;

    @Column(name = "contact_urgence_telephone", length = 30)
    private String contactUrgenceTelephone;

    /**
     * Photo de reference du visage, stockee hors du dossier public /uploads
     * (voir FileStorageService.storePrivateImage) : chemin relatif interne, jamais expose tel quel.
     */
    @JsonIgnore
    @Column(name = "photo_faciale_chemin", length = 255)
    private String photoFacialeChemin;

    /**
     * Empreinte faciale (vecteur de 128 reels calcule par face-api), serialisee en texte.
     * Sert a la recherche par reconnaissance faciale en cas d'urgence (voir ReconnaissanceFacialeService).
     */
    @JsonIgnore
    @Column(name = "descripteur_facial", columnDefinition = "TEXT")
    private String descripteurFacial;

    @JsonIgnore
    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private DossierMedical dossierMedical;

    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<RendezVous> rendezVousList = new ArrayList<>();

    /** Un patient declare decede ne peut plus se connecter (ni utiliser un token deja emis). */
    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return super.isEnabled() && !decede;
    }
}
