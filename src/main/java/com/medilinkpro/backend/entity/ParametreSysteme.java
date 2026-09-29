package com.medilinkpro.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Parametre technique persistant (ex: cles VAPID des notifications push, generees une seule fois). */
@Entity
@Table(name = "parametres_systeme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParametreSysteme {

    @Id
    @Column(name = "cle", length = 100)
    private String cle;

    @Column(name = "valeur", columnDefinition = "TEXT", nullable = false)
    private String valeur;
}
