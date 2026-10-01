package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.AvisMedecin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AvisMedecinRepository extends JpaRepository<AvisMedecin, UUID> {

    boolean existsByRendezVousId(UUID rendezVousId);

    List<AvisMedecin> findByMedecinIdAndMasqueFalseOrderByDateCreationDesc(UUID medecinId);

    List<AvisMedecin> findByMedecinIdOrderByDateCreationDesc(UUID medecinId);

    /** [medecinId, moyenne, nombre] des avis visibles, pour une liste de medecins. */
    @Query("""
            SELECT a.medecin.id, AVG(a.note), COUNT(a) FROM AvisMedecin a
            WHERE a.masque = false AND a.medecin.id IN :medecinIds
            GROUP BY a.medecin.id
            """)
    List<Object[]> resumes(@Param("medecinIds") Collection<UUID> medecinIds);

    @Query("SELECT a.rendezVous.id FROM AvisMedecin a WHERE a.patient.id = :patientId")
    List<UUID> rendezVousNotes(@Param("patientId") UUID patientId);
}
