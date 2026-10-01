package com.medilinkpro.backend.service;

import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.enums.StatutRendezVous;
import com.medilinkpro.backend.enums.TypeConsultation;
import com.medilinkpro.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Rappels automatiques des rendez-vous confirmes, pour limiter les absences :
 * la veille (dans les 24 h) et une heure avant, par notification dans l'application, push et SMS.
 * Verifie toutes les 5 minutes ; chaque rappel n'est envoye qu'une fois (rappelEnvoye / rappelProcheEnvoye).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RappelsRendezVousPlanificateur {

    private final RendezVousRepository rendezVousRepository;
    private final NotificationService notificationService;

    @Value("${medilinkpro.fuseau-horaire:Africa/Douala}")
    private String fuseau;

    @Scheduled(fixedDelayString = "${medilinkpro.rappels-rdv.intervalle-ms:300000}", initialDelay = 60000)
    public void executer() {
        envoyerRappels(LocalDateTime.now(Clock.system(ZoneId.of(fuseau))));
    }

    @Transactional
    public int envoyerRappels(LocalDateTime maintenant) {
        int envoyes = 0;
        for (RendezVous rdv : rendezVousRepository.findByStatutAndDateHeureBetween(
                StatutRendezVous.CONFIRME, maintenant, maintenant.plusHours(24))) {
            boolean dansLHeure = !rdv.getDateHeure().isAfter(maintenant.plusHours(1));
            if (dansLHeure && !rdv.isRappelProcheEnvoye()) {
                rappeler(rdv, "Votre rendez-vous approche",
                        "Rappel : rendez-vous " + (rdv.getType() == TypeConsultation.TELECONSULTATION
                                ? "en téléconsultation (rejoignez la salle depuis l'application)" : "au cabinet")
                                + " avec le Dr " + rdv.getMedecin().getNom() + " a " + String.format("%02dh%02d",
                                rdv.getDateHeure().getHour(), rdv.getDateHeure().getMinute()) + ".");
                rdv.setRappelProcheEnvoye(true);
                rdv.setRappelEnvoye(true);
                envoyes++;
            } else if (!dansLHeure && !rdv.isRappelEnvoye()) {
                rappeler(rdv, "Rappel de rendez-vous",
                        "Rappel : rendez-vous avec le Dr " + rdv.getMedecin().getNom() + " le "
                                + RendezVousService.formater(rdv.getDateHeure())
                                + ". En cas d'empêchement, annulez-le dans l'application pour libérer le créneau.");
                rdv.setRappelEnvoye(true);
                envoyes++;
            }
        }
        if (envoyes > 0) {
            log.info("{} rappel(s) de rendez-vous envoyé(s)", envoyes);
        }
        return envoyes;
    }

    private void rappeler(RendezVous rdv, String titre, String message) {
        notificationService.notifier(rdv.getPatient(), titre, message, "/patient/rendez-vous", true);
    }
}
