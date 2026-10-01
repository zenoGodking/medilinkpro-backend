package com.medilinkpro.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Indicateurs du tableau de bord (directeur : ses etablissements ; admin : toute la plateforme). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatistiquesResponse {

    private int periodeJours;

    private long etablissements;
    private long medecins;
    private long infirmiers;
    private long patients;
    private long comptesEnAttente;
    private long demandesAdhesionEnAttente;

    /** Rendez-vous dont l'heure tombe dans la periode passee. */
    private long rendezVous;
    private Map<String, Long> rendezVousParStatut;
    private long teleconsultations;
    /** Absences / (rendez-vous effectues + absences), en %. null sans donnees. */
    private Double tauxAbsence;
    /** Demandes acceptees / demandes traitees par les medecins, en %. */
    private Double tauxAcceptation;
    private long rendezVousAVenir7Jours;
    private long demandesEnAttente;
    private List<PointJour> rendezVousParJour;

    private Double noteMoyenneMedecins;
    private List<MedecinActif> medecinsLesPlusSollicites;

    private long alertes;
    private long alertesPrisesEnCharge;
    /** Delai moyen entre l'alerte et la reponse d'une infirmiere, en minutes. */
    private Double delaiMoyenReponseAlerteMinutes;
    private Double noteMoyenneInfirmieres;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PointJour {
        private LocalDate date;
        private long total;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedecinActif {
        private String nomComplet;
        private String specialite;
        private long rendezVous;
    }
}
