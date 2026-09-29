package com.medilinkpro.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Pharmacien(ne) : verifie et delivre les ordonnances en scannant leur QR code.
 * N'a aucun acces au carnet medical. Compte soumis a validation Admin.
 */
@Entity
@Table(name = "pharmaciens")
@DiscriminatorValue("PHARMACIEN")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Pharmacien extends Utilisateur {

    @Column(name = "nom_pharmacie", length = 150)
    private String nomPharmacie;

    @Column(name = "numero_agrement", length = 60)
    private String numeroAgrement;

    @Column(name = "ville", length = 100)
    private String ville;
}
