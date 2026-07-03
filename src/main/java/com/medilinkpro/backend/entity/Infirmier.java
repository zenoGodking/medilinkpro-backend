package com.medilinkpro.backend.entity;

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
}
