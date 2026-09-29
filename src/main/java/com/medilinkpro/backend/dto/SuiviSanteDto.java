package com.medilinkpro.backend.dto;

import com.medilinkpro.backend.entity.suivi.Grossesse;
import com.medilinkpro.backend.entity.suivi.Vaccination;
import com.medilinkpro.backend.enums.NiveauMesure;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.TypeMesure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Requetes et reponses du module de suivi (mesures, medicaments, vaccins, grossesse). */
public final class SuiviSanteDto {

    private SuiviSanteDto() {
    }

    // ---------------------------------------------------------------- Mesures

    public record MesureRequest(
            @NotNull TypeMesure type,
            @NotNull Double valeur,
            Double valeur2,
            Boolean aJeun,
            @PastOrPresent LocalDateTime dateMesure,
            @Size(max = 500) String note) {
    }

    public record MesureResponse(
            UUID id, TypeMesure type, Double valeur, Double valeur2, Boolean aJeun, String unite,
            LocalDateTime dateMesure, String note, NiveauMesure niveau, String interpretation,
            Role saisieParRole, boolean saisieParMoi) {
    }

    // ---------------------------------------------------------------- Rappels de medicaments

    public record RappelRequest(
            @NotBlank @Size(max = 200) String medicament,
            @Size(max = 200) String dosage,
            @NotEmpty List<LocalTime> heures,
            LocalDate dateDebut,
            LocalDate dateFin) {
    }

    public record RappelResponse(
            UUID id, String medicament, String dosage, List<LocalTime> heures,
            LocalDate dateDebut, LocalDate dateFin, boolean actif) {
    }

    // ---------------------------------------------------------------- Vaccins

    public record VaccinationRequest(
            @NotBlank @Size(max = 150) String vaccin,
            @Size(max = 60) String dose,
            @NotNull @PastOrPresent LocalDate dateVaccination,
            @Size(max = 60) String lot,
            @Size(max = 200) String lieu) {
    }

    public record VaccinationResponse(
            UUID id, String vaccin, String dose, LocalDate dateVaccination, String lot, String lieu,
            Vaccination.Statut statut, String valideParNom) {
    }

    // ---------------------------------------------------------------- Grossesse

    public record GrossesseRequest(@NotNull @PastOrPresent LocalDate dateDernieresRegles) {
    }

    public record FinGrossesseRequest(
            @NotNull Grossesse.Statut statut,
            @NotNull @PastOrPresent LocalDate dateFin,
            @Size(max = 500) String issue) {
    }

    public record VisitePrenataleRequest(
            @NotNull @PastOrPresent LocalDate date,
            Double poids,
            Integer tensionSystolique,
            Integer tensionDiastolique,
            Double hauteurUterineCm,
            String notes) {
    }

    public record VisitePrenataleResponse(
            UUID id, LocalDate date, String ageGestationnel, Double poids,
            Integer tensionSystolique, Integer tensionDiastolique, Double hauteurUterineCm,
            String notes, String medecinNom) {
    }

    /** Contact prenatal recommande par l'OMS (8 contacts : 12, 20, 26, 30, 34, 36, 38, 40 SA). */
    public record ContactPrenatal(int semaine, LocalDate datePrevue, boolean passe) {
    }

    public record GrossesseResponse(
            UUID id, LocalDate dateDernieresRegles, LocalDate dateTermePrevue,
            Integer semainesAmenorrhee, Integer joursAmenorrhee, Integer trimestre,
            Grossesse.Statut statut, LocalDate dateFin, String issue,
            List<ContactPrenatal> contactsRecommandes, List<VisitePrenataleResponse> visites) {
    }
}
