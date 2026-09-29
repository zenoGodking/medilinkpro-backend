package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.Ordonnance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdonnanceRepository extends JpaRepository<Ordonnance, UUID> {

    Optional<Ordonnance> findByConsultationId(UUID consultationId);

    List<Ordonnance> findByPatientId(UUID patientId);

    Optional<Ordonnance> findByJetonVerification(String jetonVerification);

    List<Ordonnance> findByPharmacienIdOrderByDateDelivranceDesc(UUID pharmacienId);

    /**
     * Marque l'ordonnance delivree uniquement si elle ne l'est pas deja : deux pharmacies qui
     * scannent en meme temps ne peuvent pas la delivrer deux fois. Retourne 0 si deja delivree.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Ordonnance o SET o.dateDelivrance = :date, o.pharmacienId = :pharmacienId, o.delivreePar = :delivreePar
            WHERE o.id = :id AND o.dateDelivrance IS NULL
            """)
    int delivrerSiDisponible(@Param("id") UUID id, @Param("date") LocalDateTime date,
                             @Param("pharmacienId") UUID pharmacienId, @Param("delivreePar") String delivreePar);
}
