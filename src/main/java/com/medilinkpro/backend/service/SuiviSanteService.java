package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.SuiviSanteDto.*;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.entity.suivi.Grossesse;
import com.medilinkpro.backend.entity.suivi.MesureSante;
import com.medilinkpro.backend.entity.suivi.RappelMedicament;
import com.medilinkpro.backend.entity.suivi.Vaccination;
import com.medilinkpro.backend.entity.suivi.VisitePrenatale;
import com.medilinkpro.backend.enums.NiveauMesure;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.TypeMesure;
import com.medilinkpro.backend.exception.BadRequestException;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.PatientRepository;
import com.medilinkpro.backend.repository.suivi.GrossesseRepository;
import com.medilinkpro.backend.repository.suivi.MesureSanteRepository;
import com.medilinkpro.backend.repository.suivi.RappelMedicamentRepository;
import com.medilinkpro.backend.repository.suivi.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Suivi de sante (maladies chroniques, vaccins, grossesse), rattache au carnet :
 * - lecture : memes regles que le carnet (le patient, tout medecin, l'admin) ;
 * - ecriture : le patient pour ses propres saisies (auto-mesures, vaccins declares, rappels),
 *   ou un medecin autorise (voir CarnetAccesService.verifierEcriture).
 * Les interpretations de mesures sont indicatives et ne remplacent pas un avis medical.
 */
@Service
@RequiredArgsConstructor
public class SuiviSanteService {

    /** Contacts prenatals recommandes par l'OMS (2016), en semaines d'amenorrhee. */
    static final int[] CONTACTS_OMS_SA = {12, 20, 26, 30, 34, 36, 38, 40};
    private static final int DUREE_GROSSESSE_JOURS = 280;
    private static final int MAX_SA_GROSSESSE_EN_COURS = 44;

    private final CarnetAccesService carnetAccesService;
    private final PatientRepository patientRepository;
    private final MesureSanteRepository mesureRepository;
    private final RappelMedicamentRepository rappelRepository;
    private final VaccinationRepository vaccinationRepository;
    private final GrossesseRepository grossesseRepository;

    // ================================================================ Mesures

    @Transactional(readOnly = true)
    public List<MesureResponse> mesures(UUID patientId, Utilisateur u) {
        carnetAccesService.verifierLecture(u, patientId);
        return mesureRepository.findByPatientIdOrderByDateMesureAsc(patientId).stream()
                .map(m -> toResponse(m, u)).toList();
    }

    @Transactional
    public MesureResponse ajouterMesure(UUID patientId, MesureRequest r, Utilisateur u) {
        verifierEcritureOuSoi(u, patientId);
        if (!r.type().plausible(r.valeur())) {
            throw new BadRequestException("Valeur hors des limites plausibles pour " + r.type() + " (" + r.type().getUnite() + ")");
        }
        if (r.type() == TypeMesure.TENSION) {
            if (r.valeur2() == null || !TypeMesure.TENSION.plausible(r.valeur2()) || r.valeur2() >= r.valeur()) {
                throw new BadRequestException("Tension : indiquez la systolique puis la diastolique (plus basse)");
            }
        }
        MesureSante m = mesureRepository.save(MesureSante.builder()
                .patient(getPatient(patientId))
                .type(r.type())
                .valeur(r.valeur())
                .valeur2(r.type() == TypeMesure.TENSION ? r.valeur2() : null)
                .aJeun(r.type() == TypeMesure.GLYCEMIE ? r.aJeun() : null)
                .dateMesure(r.dateMesure() != null ? r.dateMesure() : LocalDateTime.now())
                .note(r.note())
                .saisieParId(u.getId())
                .saisieParRole(u.getRole())
                .build());
        return toResponse(m, u);
    }

    @Transactional
    public void supprimerMesure(UUID mesureId, Utilisateur u) {
        MesureSante m = mesureRepository.findById(mesureId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesure introuvable"));
        if (!m.getSaisieParId().equals(u.getId())) {
            throw new AccessDeniedException("Seul l'auteur d'une mesure peut la supprimer");
        }
        mesureRepository.delete(m);
    }

    /** Lecture indicative d'une mesure (seuils usuels chez l'adulte). */
    static NiveauMesure niveau(MesureSante m) {
        double v = m.getValeur();
        return switch (m.getType()) {
            case TENSION -> {
                double dia = m.getValeur2() == null ? 0 : m.getValeur2();
                if (v >= 180 || dia >= 120) yield NiveauMesure.ALERTE;
                if (v >= 140 || dia >= 90 || v < 90 || dia < 60) yield NiveauMesure.ATTENTION;
                yield NiveauMesure.NORMAL;
            }
            case GLYCEMIE -> {
                boolean aJeun = Boolean.TRUE.equals(m.getAJeun());
                if (v < 0.70 || v >= (aJeun ? 2.5 : 3.0)) yield NiveauMesure.ALERTE;
                if (v >= (aJeun ? 1.26 : 2.0)) yield NiveauMesure.ATTENTION;
                yield NiveauMesure.NORMAL;
            }
            case TEMPERATURE -> {
                if (v < 35 || v >= 40) yield NiveauMesure.ALERTE;
                if (v >= 38) yield NiveauMesure.ATTENTION;
                yield NiveauMesure.NORMAL;
            }
            case SATURATION_O2 -> {
                if (v < 90) yield NiveauMesure.ALERTE;
                if (v < 95) yield NiveauMesure.ATTENTION;
                yield NiveauMesure.NORMAL;
            }
            case POIDS -> NiveauMesure.NON_EVALUE;
        };
    }

    static String interpretation(MesureSante m, NiveauMesure niveau) {
        if (niveau == NiveauMesure.NORMAL || niveau == NiveauMesure.NON_EVALUE) {
            return null;
        }
        double v = m.getValeur();
        return switch (m.getType()) {
            case TENSION -> v < 90 || (m.getValeur2() != null && m.getValeur2() < 60) ? "Tension basse"
                    : niveau == NiveauMesure.ALERTE ? "Tension très élevée : consultez rapidement" : "Tension élevée";
            case GLYCEMIE -> v < 0.70 ? "Hypoglycémie : prenez du sucre et consultez si cela persiste"
                    : niveau == NiveauMesure.ALERTE ? "Glycémie très élevée : consultez rapidement" : "Glycémie élevée";
            case TEMPERATURE -> v < 35 ? "Température très basse" : niveau == NiveauMesure.ALERTE ? "Fièvre très élevée" : "Fievre";
            case SATURATION_O2 -> niveau == NiveauMesure.ALERTE ? "Oxygène très bas : urgence" : "Oxygène un peu bas";
            case POIDS -> null;
        };
    }

    private MesureResponse toResponse(MesureSante m, Utilisateur u) {
        NiveauMesure niveau = niveau(m);
        return new MesureResponse(m.getId(), m.getType(), m.getValeur(), m.getValeur2(), m.getAJeun(),
                m.getType().getUnite(), m.getDateMesure(), m.getNote(), niveau, interpretation(m, niveau),
                m.getSaisieParRole(), m.getSaisieParId().equals(u.getId()));
    }

    // ================================================================ Rappels de medicaments

    @Transactional(readOnly = true)
    public List<RappelResponse> rappels(UUID patientId, Utilisateur u) {
        carnetAccesService.verifierLecture(u, patientId);
        return rappelRepository.findByPatientIdOrderByMedicamentAsc(patientId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RappelResponse ajouterRappel(UUID patientId, RappelRequest r, Utilisateur u) {
        verifierSoi(u, patientId);
        LocalDate debut = r.dateDebut() != null ? r.dateDebut() : LocalDate.now();
        if (r.dateFin() != null && r.dateFin().isBefore(debut)) {
            throw new BadRequestException("La date de fin précède la date de début");
        }
        RappelMedicament rappel = rappelRepository.save(RappelMedicament.builder()
                .patient(getPatient(patientId))
                .medicament(r.medicament())
                .dosage(r.dosage())
                .heures(new ArrayList<>(r.heures().stream().distinct().sorted().toList()))
                .dateDebut(debut)
                .dateFin(r.dateFin())
                .build());
        return toResponse(rappel);
    }

    @Transactional
    public RappelResponse basculerRappel(UUID rappelId, Utilisateur u) {
        RappelMedicament r = getRappel(rappelId);
        verifierSoi(u, r.getPatient().getId());
        r.setActif(!r.isActif());
        return toResponse(r);
    }

    @Transactional
    public void supprimerRappel(UUID rappelId, Utilisateur u) {
        RappelMedicament r = getRappel(rappelId);
        verifierSoi(u, r.getPatient().getId());
        rappelRepository.delete(r);
    }

    private RappelMedicament getRappel(UUID id) {
        return rappelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rappel introuvable"));
    }

    private RappelResponse toResponse(RappelMedicament r) {
        return new RappelResponse(r.getId(), r.getMedicament(), r.getDosage(), List.copyOf(r.getHeures()),
                r.getDateDebut(), r.getDateFin(), r.isActif());
    }

    // ================================================================ Vaccins

    @Transactional(readOnly = true)
    public List<VaccinationResponse> vaccinations(UUID patientId, Utilisateur u) {
        carnetAccesService.verifierLecture(u, patientId);
        return vaccinationRepository.findByPatientIdOrderByDateVaccinationDesc(patientId).stream().map(this::toResponse).toList();
    }

    /** Declaree si le patient l'ajoute lui-meme, validee si c'est un medecin autorise. */
    @Transactional
    public VaccinationResponse ajouterVaccination(UUID patientId, VaccinationRequest r, Utilisateur u) {
        verifierEcritureOuSoi(u, patientId);
        boolean parMedecin = u.getRole() == Role.MEDECIN;
        Vaccination v = vaccinationRepository.save(Vaccination.builder()
                .patient(getPatient(patientId))
                .vaccin(r.vaccin()).dose(r.dose()).dateVaccination(r.dateVaccination()).lot(r.lot()).lieu(r.lieu())
                .statut(parMedecin ? Vaccination.Statut.VALIDEE : Vaccination.Statut.DECLAREE)
                .valideParId(parMedecin ? u.getId() : null)
                .valideParNom(parMedecin ? "Dr " + u.getPrenom() + " " + u.getNom() : null)
                .build());
        return toResponse(v);
    }

    /** Un medecin autorise confirme un vaccin declare par le patient. */
    @Transactional
    public VaccinationResponse validerVaccination(UUID vaccinationId, Utilisateur u) {
        Vaccination v = vaccinationRepository.findById(vaccinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination introuvable"));
        carnetAccesService.verifierEcriture(u, v.getPatient().getId());
        v.setStatut(Vaccination.Statut.VALIDEE);
        v.setValideParId(u.getId());
        v.setValideParNom("Dr " + u.getPrenom() + " " + u.getNom());
        return toResponse(v);
    }

    /** Le patient peut retirer un vaccin qu'il a declare ; un vaccin valide par un medecin reste. */
    @Transactional
    public void supprimerVaccination(UUID vaccinationId, Utilisateur u) {
        Vaccination v = vaccinationRepository.findById(vaccinationId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination introuvable"));
        verifierSoi(u, v.getPatient().getId());
        if (v.getStatut() == Vaccination.Statut.VALIDEE) {
            throw new BadRequestException("Un vaccin validé par un médecin ne peut pas être supprimé");
        }
        vaccinationRepository.delete(v);
    }

    private VaccinationResponse toResponse(Vaccination v) {
        return new VaccinationResponse(v.getId(), v.getVaccin(), v.getDose(), v.getDateVaccination(), v.getLot(),
                v.getLieu(), v.getStatut(), v.getValideParNom());
    }

    // ================================================================ Grossesse

    @Transactional(readOnly = true)
    public List<GrossesseResponse> grossesses(UUID patientId, Utilisateur u) {
        carnetAccesService.verifierLecture(u, patientId);
        return grossesseRepository.findByPatientIdOrderByDateDernieresReglesDesc(patientId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public GrossesseResponse declarerGrossesse(UUID patientId, GrossesseRequest r, Utilisateur u) {
        verifierEcritureOuSoi(u, patientId);
        long semaines = ChronoUnit.DAYS.between(r.dateDernieresRegles(), LocalDate.now()) / 7;
        if (semaines > MAX_SA_GROSSESSE_EN_COURS) {
            throw new BadRequestException("Date des dernières règles trop ancienne pour une grossesse en cours");
        }
        boolean dejaEnCours = grossesseRepository.findByPatientIdOrderByDateDernieresReglesDesc(patientId).stream()
                .anyMatch(g -> g.getStatut() == Grossesse.Statut.EN_COURS);
        if (dejaEnCours) {
            throw new BadRequestException("Une grossesse est déjà en cours de suivi");
        }
        return toResponse(grossesseRepository.save(Grossesse.builder()
                .patient(getPatient(patientId))
                .dateDernieresRegles(r.dateDernieresRegles())
                .build()));
    }

    @Transactional
    public GrossesseResponse terminerGrossesse(UUID grossesseId, FinGrossesseRequest r, Utilisateur u) {
        Grossesse g = getGrossesse(grossesseId);
        verifierEcritureOuSoi(u, g.getPatient().getId());
        if (r.statut() == Grossesse.Statut.EN_COURS) {
            throw new BadRequestException("Statut de fin invalide");
        }
        g.setStatut(r.statut());
        g.setDateFin(r.dateFin());
        g.setIssue(r.issue());
        return toResponse(g);
    }

    /** Consultation prenatale : reservee a un medecin autorise. */
    @Transactional
    public GrossesseResponse ajouterVisite(UUID grossesseId, VisitePrenataleRequest r, Utilisateur u) {
        Grossesse g = getGrossesse(grossesseId);
        carnetAccesService.verifierEcriture(u, g.getPatient().getId());
        if (r.date().isBefore(g.getDateDernieresRegles())) {
            throw new BadRequestException("La visite précède le début de la grossesse");
        }
        g.getVisites().add(VisitePrenatale.builder()
                .grossesse(g).date(r.date()).poids(r.poids())
                .tensionSystolique(r.tensionSystolique()).tensionDiastolique(r.tensionDiastolique())
                .hauteurUterineCm(r.hauteurUterineCm()).notes(r.notes())
                .medecinId(u.getId()).medecinNom("Dr " + u.getPrenom() + " " + u.getNom())
                .build());
        grossesseRepository.flush();
        return toResponse(g);
    }

    private Grossesse getGrossesse(UUID id) {
        return grossesseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Grossesse introuvable"));
    }

    GrossesseResponse toResponse(Grossesse g) {
        LocalDate ddr = g.getDateDernieresRegles();
        LocalDate reference = g.getStatut() == Grossesse.Statut.EN_COURS ? LocalDate.now() : g.getDateFin();
        long jours = ChronoUnit.DAYS.between(ddr, reference);
        int sa = (int) (jours / 7);
        Integer trimestre = g.getStatut() != Grossesse.Statut.EN_COURS ? null : sa < 14 ? 1 : sa < 28 ? 2 : 3;
        List<ContactPrenatal> contacts = new ArrayList<>();
        for (int semaine : CONTACTS_OMS_SA) {
            LocalDate date = ddr.plusWeeks(semaine);
            contacts.add(new ContactPrenatal(semaine, date, date.isBefore(LocalDate.now())));
        }
        List<VisitePrenataleResponse> visites = g.getVisites().stream().map(v -> {
            long j = ChronoUnit.DAYS.between(ddr, v.getDate());
            return new VisitePrenataleResponse(v.getId(), v.getDate(), (j / 7) + " SA + " + (j % 7) + " j",
                    v.getPoids(), v.getTensionSystolique(), v.getTensionDiastolique(), v.getHauteurUterineCm(),
                    v.getNotes(), v.getMedecinNom());
        }).toList();
        return new GrossesseResponse(g.getId(), ddr, ddr.plusDays(DUREE_GROSSESSE_JOURS),
                sa, (int) (jours % 7), trimestre, g.getStatut(), g.getDateFin(), g.getIssue(), contacts, visites);
    }

    // ================================================================ Regles

    /** Le patient pour lui-meme, ou un medecin autorise a ecrire dans le carnet. */
    private void verifierEcritureOuSoi(Utilisateur u, UUID patientId) {
        if (u.getRole() == Role.PATIENT && u.getId().equals(patientId)) {
            return;
        }
        carnetAccesService.verifierEcriture(u, patientId);
    }

    private void verifierSoi(Utilisateur u, UUID patientId) {
        if (!u.getId().equals(patientId)) {
            throw new AccessDeniedException("Reserve au patient concerné");
        }
    }

    private Patient getPatient(UUID id) {
        return patientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patient non trouvé"));
    }
}
