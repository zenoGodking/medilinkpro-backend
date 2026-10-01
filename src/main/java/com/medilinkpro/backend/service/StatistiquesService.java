package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.response.StatistiquesResponse;
import com.medilinkpro.backend.entity.AlerteSoinDomicile;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import com.medilinkpro.backend.enums.StatutDemandeIntegration;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.enums.TypeConsultation;
import com.medilinkpro.backend.repository.AlerteSoinDomicileRepository;
import com.medilinkpro.backend.repository.AvisMedecinRepository;
import com.medilinkpro.backend.repository.DemandeIntegrationRepository;
import com.medilinkpro.backend.repository.EtablissementRepository;
import com.medilinkpro.backend.repository.InfirmierRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import com.medilinkpro.backend.repository.PatientRepository;
import com.medilinkpro.backend.repository.RendezVousRepository;
import com.medilinkpro.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tableau de bord : indicateurs calcules sur les 30 derniers jours.
 * Le directeur ne voit que l'activite de ses etablissements (medecins et infirmieres rattaches,
 * rendez-vous qui y sont pris) ; l'administrateur voit toute la plateforme.
 */
@Service
@RequiredArgsConstructor
public class StatistiquesService {

    public static final int PERIODE_JOURS = 30;
    private static final Set<StatutRendezVous> ACCEPTES = Set.of(StatutRendezVous.CONFIRME, StatutRendezVous.TERMINE, StatutRendezVous.NO_SHOW);

    private final RendezVousRepository rendezVousRepository;
    private final MedecinRepository medecinRepository;
    private final InfirmierRepository infirmierRepository;
    private final PatientRepository patientRepository;
    private final EtablissementRepository etablissementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeIntegrationRepository demandeRepository;
    private final AlerteSoinDomicileRepository alerteRepository;
    private final AvisMedecinRepository avisRepository;

    @Value("${medilinkpro.fuseau-horaire:Africa/Douala}")
    private String fuseau;

    @Transactional(readOnly = true)
    public StatistiquesResponse statistiques(Utilisateur u) {
        LocalDateTime maintenant = LocalDateTime.now(Clock.system(ZoneId.of(fuseau)));
        LocalDateTime debut = maintenant.toLocalDate().minusDays(PERIODE_JOURS - 1L).atStartOfDay();
        LocalDateTime finAVenir = maintenant.plusDays(7);

        List<RendezVous> rdvs;
        List<Medecin> medecins;
        List<Infirmier> infirmiers;
        long etablissements;
        long patients;
        long comptesEnAttente = 0;
        long demandesAdhesion;
        List<AlerteSoinDomicile> alertes;

        if (u.getRole() == Role.ADMIN) {
            rdvs = rendezVousRepository.findByDateHeureBetween(debut, finAVenir);
            medecins = medecinRepository.findAll();
            infirmiers = infirmierRepository.findAll();
            etablissements = etablissementRepository.count();
            patients = patientRepository.count();
            comptesEnAttente = utilisateurRepository.findAll().stream()
                    .filter(x -> x.getStatutCompte() == StatutCompte.EN_ATTENTE).count();
            demandesAdhesion = demandeRepository.findByStatutOrderByDateCreationDesc(StatutDemandeIntegration.EN_ATTENTE).size();
            alertes = alerteRepository.findByDateCreationAfter(debut);
        } else if (u.getRole() == Role.DIRECTEUR) {
            rdvs = rendezVousRepository.findDansEtablissementsDuDirecteur(u.getId()).stream()
                    .filter(r -> !r.getDateHeure().isBefore(debut) && !r.getDateHeure().isAfter(finAVenir)).toList();
            medecins = medecinRepository.findByEtablissement_Directeur_Id(u.getId());
            infirmiers = infirmierRepository.findByEtablissement_Directeur_Id(u.getId());
            etablissements = etablissementRepository.findByDirecteurId(u.getId()).size();
            patients = rendezVousRepository.findDansEtablissementsDuDirecteur(u.getId()).stream()
                    .map(r -> r.getPatient().getId()).distinct().count();
            demandesAdhesion = demandeRepository.findByEtablissement_Directeur_IdAndStatutOrderByDateCreationDesc(
                    u.getId(), StatutDemandeIntegration.EN_ATTENTE).size();
            Set<UUID> sesInfirmieres = infirmiers.stream().map(Infirmier::getId).collect(Collectors.toSet());
            alertes = alerteRepository.findByDateCreationAfter(debut).stream()
                    .filter(a -> a.getInfirmier() != null && sesInfirmieres.contains(a.getInfirmier().getId())).toList();
        } else {
            throw new AccessDeniedException("Réservé au directeur et à l'administrateur");
        }

        List<RendezVous> passes = rdvs.stream().filter(r -> !r.getDateHeure().isAfter(maintenant)).toList();
        Map<StatutRendezVous, Long> parStatut = new EnumMap<>(StatutRendezVous.class);
        passes.forEach(r -> parStatut.merge(r.getStatut(), 1L, Long::sum));
        long termines = parStatut.getOrDefault(StatutRendezVous.TERMINE, 0L);
        long absences = parStatut.getOrDefault(StatutRendezVous.NO_SHOW, 0L);
        long acceptes = rdvs.stream().filter(r -> ACCEPTES.contains(r.getStatut())).count();
        long refuses = rdvs.stream().filter(r -> r.getStatut() == StatutRendezVous.REFUSE).count();

        Map<LocalDate, Long> parJour = new LinkedHashMap<>();
        for (int i = 0; i < PERIODE_JOURS; i++) {
            parJour.put(debut.toLocalDate().plusDays(i), 0L);
        }
        passes.forEach(r -> parJour.computeIfPresent(r.getDateHeure().toLocalDate(), (d, n) -> n + 1));

        Map<UUID, Long> rdvParMedecin = passes.stream()
                .collect(Collectors.groupingBy(r -> r.getMedecin().getId(), Collectors.counting()));
        Map<UUID, Medecin> medecinsParId = passes.stream().map(RendezVous::getMedecin)
                .collect(Collectors.toMap(Medecin::getId, Function.identity(), (a, b) -> a));
        List<StatistiquesResponse.MedecinActif> top = rdvParMedecin.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Medecin m = medecinsParId.get(e.getKey());
                    return StatistiquesResponse.MedecinActif.builder()
                            .nomComplet("Dr " + m.getPrenom() + " " + m.getNom())
                            .specialite(m.getSpecialite())
                            .rendezVous(e.getValue())
                            .build();
                }).toList();

        List<Object[]> resumesAvis = medecins.isEmpty() ? List.of()
                : avisRepository.resumes(medecins.stream().map(Medecin::getId).toList());
        long nbAvis = resumesAvis.stream().mapToLong(r -> ((Number) r[2]).longValue()).sum();
        Double noteMedecins = nbAvis == 0 ? null : arrondir(resumesAvis.stream()
                .mapToDouble(r -> ((Number) r[1]).doubleValue() * ((Number) r[2]).longValue()).sum() / nbAvis);

        List<AlerteSoinDomicile> prises = alertes.stream().filter(a -> a.getDateReponse() != null).toList();
        Double delai = prises.isEmpty() ? null : arrondir(prises.stream()
                .mapToLong(a -> Duration.between(a.getDateCreation(), a.getDateReponse()).toSeconds()).average().orElse(0) / 60.0);
        List<Integer> notesInf = alertes.stream().map(AlerteSoinDomicile::getNote).filter(n -> n != null).toList();

        Map<String, Long> statuts = new LinkedHashMap<>();
        parStatut.forEach((k, v) -> statuts.put(k.name(), v));

        return StatistiquesResponse.builder()
                .periodeJours(PERIODE_JOURS)
                .etablissements(etablissements)
                .medecins(medecins.size())
                .infirmiers(infirmiers.size())
                .patients(patients)
                .comptesEnAttente(comptesEnAttente)
                .demandesAdhesionEnAttente(demandesAdhesion)
                .rendezVous(passes.size())
                .rendezVousParStatut(statuts)
                .teleconsultations(passes.stream().filter(r -> r.getType() == TypeConsultation.TELECONSULTATION).count())
                .tauxAbsence(termines + absences == 0 ? null : arrondir(100.0 * absences / (termines + absences)))
                .tauxAcceptation(acceptes + refuses == 0 ? null : arrondir(100.0 * acceptes / (acceptes + refuses)))
                .rendezVousAVenir7Jours(rdvs.stream().filter(r -> r.getDateHeure().isAfter(maintenant)
                        && (r.getStatut() == StatutRendezVous.CONFIRME || r.getStatut() == StatutRendezVous.EN_ATTENTE)).count())
                .demandesEnAttente(rdvs.stream().filter(r -> r.getStatut() == StatutRendezVous.EN_ATTENTE
                        && r.getDateHeure().isAfter(maintenant)).count())
                .rendezVousParJour(parJour.entrySet().stream()
                        .map(e -> new StatistiquesResponse.PointJour(e.getKey(), e.getValue())).collect(Collectors.toCollection(ArrayList::new)))
                .noteMoyenneMedecins(noteMedecins)
                .medecinsLesPlusSollicites(top)
                .alertes(alertes.size())
                .alertesPrisesEnCharge(prises.size())
                .delaiMoyenReponseAlerteMinutes(delai)
                .noteMoyenneInfirmieres(notesInf.isEmpty() ? null
                        : arrondir(notesInf.stream().mapToInt(Integer::intValue).average().orElse(0)))
                .build();
    }

    private static Double arrondir(double v) {
        return Math.round(v * 10) / 10.0;
    }

}
