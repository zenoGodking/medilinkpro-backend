package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.request.AbsenceRequest;
import com.medilinkpro.backend.dto.request.DisponibilitesRequest;
import com.medilinkpro.backend.dto.request.PlageDisponibiliteRequest;
import com.medilinkpro.backend.dto.response.CreneauResponse;
import com.medilinkpro.backend.dto.response.DisponibilitesResponse;
import com.medilinkpro.backend.entity.AbsenceMedecin;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.PlageDisponibilite;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.AbsenceMedecinRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import com.medilinkpro.backend.repository.PlageDisponibiliteRepository;
import com.medilinkpro.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Calendrier de disponibilite des medecins : semaine type (plages horaires) + absences.
 * Les creneaux reservables par les patients sont generes a partir de ces plages ; un
 * rendez-vous ne peut etre pris que sur le debut d'un creneau libre (voir verifierCreneau).
 * Tant qu'un medecin n'a pas defini sa semaine, les heures ouvrables par defaut s'appliquent :
 * du lundi au vendredi, 08:00-12:00 et 14:00-17:00, creneaux de 30 minutes.
 */
@Service
@RequiredArgsConstructor
public class DisponibiliteService {

    private static final int DUREE_PAR_DEFAUT = 30;
    private static final int JOURS_MAX_PAR_REQUETE = 31;

    private final PlageDisponibiliteRepository plageRepository;
    private final AbsenceMedecinRepository absenceRepository;
    private final MedecinRepository medecinRepository;
    private final RendezVousRepository rendezVousRepository;

    @Value("${medilinkpro.fuseau-horaire:Africa/Douala}")
    private String fuseau;

    // ------------------------------------------------------------------ Lecture

    @Transactional(readOnly = true)
    public DisponibilitesResponse disponibilites(UUID medecinId) {
        getMedecin(medecinId);
        List<PlageDisponibilite> enregistrees = plageRepository.findByMedecinIdOrderByJourSemaineAscHeureDebutAsc(medecinId);
        return DisponibilitesResponse.builder()
                .medecinId(medecinId)
                .parDefaut(enregistrees.isEmpty())
                .plages(plagesEffectives(enregistrees).stream().map(DisponibiliteService::toPlage).toList())
                .absences(absenceRepository.findByMedecinIdOrderByDateDebutAsc(medecinId).stream()
                        .filter(a -> !a.getDateFin().isBefore(aujourdhui()))
                        .map(DisponibiliteService::toAbsence).toList())
                .build();
    }

    /** Creneaux de [du, au] (bornes incluses), hors absences et heures passees, avec leur etat libre/pris. */
    @Transactional(readOnly = true)
    public List<CreneauResponse> creneaux(UUID medecinId, LocalDate du, LocalDate au) {
        getMedecin(medecinId);
        if (au.isBefore(du)) {
            throw new BadRequestException("La date de fin doit suivre la date de début");
        }
        if (ChronoUnit.DAYS.between(du, au) >= JOURS_MAX_PAR_REQUETE) {
            throw new BadRequestException("Intervalle limité à " + JOURS_MAX_PAR_REQUETE + " jours");
        }

        List<PlageDisponibilite> plages = plagesEffectives(
                plageRepository.findByMedecinIdOrderByJourSemaineAscHeureDebutAsc(medecinId));
        List<AbsenceMedecin> absences = absenceRepository
                .findByMedecinIdAndDateFinGreaterThanEqualAndDateDebutLessThanEqual(medecinId, du, au);
        Set<LocalDateTime> reservees = new HashSet<>(rendezVousRepository.findHeuresReservees(
                medecinId, du.atStartOfDay(), au.plusDays(1).atStartOfDay()));
        LocalDateTime maintenant = maintenant();

        List<CreneauResponse> resultat = new ArrayList<>();
        for (LocalDate jour = du; !jour.isAfter(au); jour = jour.plusDays(1)) {
            if (estAbsent(absences, jour)) {
                continue;
            }
            for (PlageDisponibilite plage : plages) {
                if (plage.getJourSemaine() != jour.getDayOfWeek()) {
                    continue;
                }
                for (LocalDateTime debut : debutsDeCreneaux(plage, jour)) {
                    if (!debut.isAfter(maintenant)) {
                        continue;
                    }
                    resultat.add(CreneauResponse.builder()
                            .debut(debut)
                            .fin(debut.plusMinutes(plage.getDureeCreneauMinutes()))
                            .libre(!reservees.contains(debut))
                            .build());
                }
            }
        }
        resultat.sort(Comparator.comparing(CreneauResponse::getDebut));
        return resultat;
    }

    /** Refuse une heure de rendez-vous qui ne correspond pas au debut d'un creneau ouvert du medecin. */
    @Transactional(readOnly = true)
    public void verifierCreneau(UUID medecinId, LocalDateTime dateHeure) {
        LocalDate jour = dateHeure.toLocalDate();
        if (estAbsent(absenceRepository.findByMedecinIdAndDateFinGreaterThanEqualAndDateDebutLessThanEqual(
                medecinId, jour, jour), jour)) {
            throw new BadRequestException("Le médecin est absent ce jour-la. Veuillez choisir une autre date.");
        }
        boolean ouvert = plagesEffectives(plageRepository.findByMedecinIdOrderByJourSemaineAscHeureDebutAsc(medecinId))
                .stream()
                .filter(p -> p.getJourSemaine() == jour.getDayOfWeek())
                .anyMatch(p -> debutsDeCreneaux(p, jour).contains(dateHeure.truncatedTo(ChronoUnit.MINUTES))
                        && dateHeure.getSecond() == 0 && dateHeure.getNano() == 0);
        if (!ouvert) {
            throw new BadRequestException(
                    "Cet horaire ne fait pas partie des heures de consultation du médecin. Choisissez un créneau proposé.");
        }
    }

    // ------------------------------------------------------------------ Ecriture (le medecin)

    @Transactional
    public DisponibilitesResponse definirSemaine(UUID medecinId, DisponibilitesRequest request) {
        Medecin medecin = getMedecin(medecinId);
        List<PlageDisponibiliteRequest> plages = request.getPlages();
        valider(plages);

        plageRepository.supprimerParMedecin(medecinId);
        plageRepository.flush();
        plageRepository.saveAll(plages.stream().map(p -> PlageDisponibilite.builder()
                .medecin(medecin)
                .jourSemaine(p.getJourSemaine())
                .heureDebut(p.getHeureDebut().truncatedTo(ChronoUnit.MINUTES))
                .heureFin(p.getHeureFin().truncatedTo(ChronoUnit.MINUTES))
                .dureeCreneauMinutes(p.getDureeCreneauMinutes())
                .build()).toList());
        return disponibilites(medecinId);
    }

    @Transactional
    public DisponibilitesResponse.Absence ajouterAbsence(UUID medecinId, AbsenceRequest request) {
        if (request.getDateFin().isBefore(request.getDateDebut())) {
            throw new BadRequestException("La date de fin doit suivre la date de début");
        }
        AbsenceMedecin absence = absenceRepository.save(AbsenceMedecin.builder()
                .medecin(getMedecin(medecinId))
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .motif(request.getMotif())
                .build());
        return toAbsence(absence);
    }

    @Transactional
    public void supprimerAbsence(UUID medecinId, UUID absenceId) {
        AbsenceMedecin absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Absence non trouvée"));
        if (!absence.getMedecin().getId().equals(medecinId)) {
            throw new AccessDeniedException("Cette absence ne vous appartient pas");
        }
        absenceRepository.delete(absence);
    }

    // ------------------------------------------------------------------ Outils

    private void valider(List<PlageDisponibiliteRequest> plages) {
        for (PlageDisponibiliteRequest p : plages) {
            if (!p.getHeureDebut().isBefore(p.getHeureFin())) {
                throw new BadRequestException("Chaque plage doit finir après son début (" + p.getJourSemaine() + ")");
            }
            if (ChronoUnit.MINUTES.between(p.getHeureDebut(), p.getHeureFin()) < p.getDureeCreneauMinutes()) {
                throw new BadRequestException("Une plage doit contenir au moins un créneau (" + p.getJourSemaine() + ")");
            }
        }
        for (int i = 0; i < plages.size(); i++) {
            for (int j = i + 1; j < plages.size(); j++) {
                PlageDisponibiliteRequest a = plages.get(i);
                PlageDisponibiliteRequest b = plages.get(j);
                if (a.getJourSemaine() == b.getJourSemaine()
                        && a.getHeureDebut().isBefore(b.getHeureFin())
                        && b.getHeureDebut().isBefore(a.getHeureFin())) {
                    throw new BadRequestException("Deux plages se chevauchent le " + a.getJourSemaine());
                }
            }
        }
    }

    private static List<PlageDisponibilite> plagesEffectives(List<PlageDisponibilite> enregistrees) {
        if (!enregistrees.isEmpty()) {
            return enregistrees;
        }
        List<PlageDisponibilite> parDefaut = new ArrayList<>();
        for (DayOfWeek jour : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            parDefaut.add(plage(jour, LocalTime.of(8, 0), LocalTime.of(12, 0)));
            parDefaut.add(plage(jour, LocalTime.of(14, 0), LocalTime.of(17, 0)));
        }
        return parDefaut;
    }

    private static PlageDisponibilite plage(DayOfWeek jour, LocalTime debut, LocalTime fin) {
        return PlageDisponibilite.builder().jourSemaine(jour).heureDebut(debut).heureFin(fin)
                .dureeCreneauMinutes(DUREE_PAR_DEFAUT).build();
    }

    private static List<LocalDateTime> debutsDeCreneaux(PlageDisponibilite plage, LocalDate jour) {
        List<LocalDateTime> debuts = new ArrayList<>();
        LocalDateTime debut = jour.atTime(plage.getHeureDebut());
        LocalDateTime fin = jour.atTime(plage.getHeureFin());
        while (!debut.plusMinutes(plage.getDureeCreneauMinutes()).isAfter(fin)) {
            debuts.add(debut);
            debut = debut.plusMinutes(plage.getDureeCreneauMinutes());
        }
        return debuts;
    }

    private static boolean estAbsent(List<AbsenceMedecin> absences, LocalDate jour) {
        return absences.stream().anyMatch(a -> !jour.isBefore(a.getDateDebut()) && !jour.isAfter(a.getDateFin()));
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(Clock.system(ZoneId.of(fuseau)));
    }

    private LocalDate aujourdhui() {
        return maintenant().toLocalDate();
    }

    private Medecin getMedecin(UUID id) {
        return medecinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médecin non trouvé avec l'id : " + id));
    }

    private static DisponibilitesResponse.Plage toPlage(PlageDisponibilite p) {
        return DisponibilitesResponse.Plage.builder()
                .jourSemaine(p.getJourSemaine())
                .heureDebut(p.getHeureDebut())
                .heureFin(p.getHeureFin())
                .dureeCreneauMinutes(p.getDureeCreneauMinutes())
                .build();
    }

    private static DisponibilitesResponse.Absence toAbsence(AbsenceMedecin a) {
        return DisponibilitesResponse.Absence.builder()
                .id(a.getId()).dateDebut(a.getDateDebut()).dateFin(a.getDateFin()).motif(a.getMotif())
                .build();
    }
}
