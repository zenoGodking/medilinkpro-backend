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
 * Secretaire medicale : enregistre les patients, planifie les consultations, gere les rappels.
 */
@Entity
@Table(name = "secretaires")
@DiscriminatorValue("SECRETAIRE")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Secretaire extends Utilisateur {
}
