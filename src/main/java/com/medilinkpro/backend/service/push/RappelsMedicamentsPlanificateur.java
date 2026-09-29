package com.medilinkpro.backend.service.push;

import com.medilinkpro.backend.entity.suivi.RappelMedicament;
import com.medilinkpro.backend.repository.suivi.RappelMedicamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Envoie les rappels de prise de medicaments a l'heure choisie par le patient, dans le fuseau
 * horaire de la plateforme (medilinkpro.fuseau-horaire, Africa/Douala par defaut). Verifie chaque
 * minute ; une prise est notifiee une seule fois, au plus tard FENETRE_MINUTES apres l'heure prevue
 * (un redemarrage du serveur ne fait pas rattraper des rappels de la veille).
 */
@Component
@RequiredArgsConstructor
public class RappelsMedicamentsPlanificateur {

    static final int FENETRE_MINUTES = 5;

    private final RappelMedicamentRepository rappelRepository;
    private final NotificationPushService notificationPushService;

    @Value("${medilinkpro.fuseau-horaire:Africa/Douala}")
    private String fuseau;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void verifier() {
        envoyerRappelsDus(LocalDateTime.now(Clock.system(ZoneId.of(fuseau))));
    }

    @Transactional
    public int envoyerRappelsDus(LocalDateTime maintenant) {
        LocalDate aujourdHui = maintenant.toLocalDate();
        int envoyes = 0;
        for (RappelMedicament r : rappelRepository.findByActifTrue()) {
            if (aujourdHui.isBefore(r.getDateDebut()) || (r.getDateFin() != null && aujourdHui.isAfter(r.getDateFin()))) {
                continue;
            }
            for (LocalTime heure : r.getHeures()) {
                LocalDateTime prevue = aujourdHui.atTime(heure);
                String cle = prevue.toString().substring(0, 16);
                boolean due = !maintenant.isBefore(prevue) && maintenant.isBefore(prevue.plusMinutes(FENETRE_MINUTES));
                if (due && !cle.equals(r.getDernierEnvoi())) {
                    notificationPushService.envoyer(r.getPatient().getId(), "Rappel de traitement",
                            "Il est l'heure de prendre " + r.getMedicament() + (r.getDosage() != null ? " (" + r.getDosage() + ")" : ""),
                            "/patient/suivi", "rappel-" + r.getId());
                    r.setDernierEnvoi(cle);
                    envoyes++;
                }
            }
        }
        return envoyes;
    }
}
