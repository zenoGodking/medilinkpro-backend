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
 * Administrateur systeme : gere les comptes, droits d'acces et parametres globaux.
 */
@Entity
@Table(name = "administrateurs")
@DiscriminatorValue("ADMIN")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Admin extends Utilisateur {
}
