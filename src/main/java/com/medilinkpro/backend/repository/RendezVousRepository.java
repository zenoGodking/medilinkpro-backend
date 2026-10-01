package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.RendezVous;
import com.medilinkpro.backend.enums.StatutRendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RendezVousRepository extends JpaRepository<RendezVous, UUID> {

    List<RendezVous> findByPatientId(UUID patientId);

    List<RendezVous> findByMedecinId(UUID medecinId);

    List<RendezVous> findByStatut(StatutRendezVous statut);

    List<RendezVous> findByDateHeureBetween(LocalDateTime debut, LocalDateTime fin);

    boolean existsByPatientIdAndMedecinIdAndStatutIn(UUID patientId, UUID medecinId, java.util.Collection<StatutRendezVous> statuts);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RendezVous r
            WHERE r.medecin.id = :medecinId
            AND r.dateHeure = :dateHeure
            AND r.statut NOT IN (com.medilinkpro.backend.enums.StatutRendezVous.ANNULE,
                                 com.medilinkpro.backend.enums.StatutRendezVous.REFUSE)
            """)
    boolean existsCreneauOccupe(@Param("medecinId") UUID medecinId, @Param("dateHeure") LocalDateTime dateHeure);

    /** Meme verification en ignorant un rendez-vous donne (celui que l'on reporte). */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RendezVous r
            WHERE r.medecin.id = :medecinId
            AND r.dateHeure = :dateHeure
            AND r.id <> :exclu
            AND r.statut NOT IN (com.medilinkpro.backend.enums.StatutRendezVous.ANNULE,
                                 com.medilinkpro.backend.enums.StatutRendezVous.REFUSE)
            """)
    boolean existsCreneauOccupeHors(@Param("medecinId") UUID medecinId, @Param("dateHeure") LocalDateTime dateHeure,
                                    @Param("exclu") UUID rendezVousExclu);

    /** Rendez-vous confirmes dont l'heure tombe dans l'intervalle (rappels). */
    List<RendezVous> findByStatutAndDateHeureBetween(StatutRendezVous statut, LocalDateTime debut, LocalDateTime fin);

    /** Heures deja reservees (rendez-vous non annules) d'un medecin sur un intervalle. */
    @Query("""
            SELECT r.dateHeure FROM RendezVous r
            WHERE r.medecin.id = :medecinId
            AND r.dateHeure >= :debut AND r.dateHeure < :fin
            AND r.statut NOT IN (com.medilinkpro.backend.enums.StatutRendezVous.ANNULE,
                                 com.medilinkpro.backend.enums.StatutRendezVous.REFUSE)
            """)
    List<LocalDateTime> findHeuresReservees(@Param("medecinId") UUID medecinId,
                                            @Param("debut") LocalDateTime debut,
                                            @Param("fin") LocalDateTime fin);

    /** Rendez-vous pris dans un etablissement du directeur, ou avec un medecin de cet etablissement. */
    @Query("""
            SELECT r FROM RendezVous r
            LEFT JOIN r.etablissement e
            LEFT JOIN r.medecin.etablissement em
            WHERE e.directeur.id = :directeurId OR (e IS NULL AND em.directeur.id = :directeurId)
            """)
    List<RendezVous> findDansEtablissementsDuDirecteur(@Param("directeurId") UUID directeurId);
}
