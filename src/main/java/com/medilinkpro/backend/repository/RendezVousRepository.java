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

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RendezVous r
            WHERE r.medecin.id = :medecinId
            AND r.dateHeure = :dateHeure
            AND r.statut <> 'ANNULE'
            """)
    boolean existsCreneauOccupe(@Param("medecinId") UUID medecinId, @Param("dateHeure") LocalDateTime dateHeure);
}
