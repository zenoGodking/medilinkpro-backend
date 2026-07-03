package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.Ordonnance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdonnanceRepository extends JpaRepository<Ordonnance, UUID> {

    Optional<Ordonnance> findByConsultationId(UUID consultationId);

    List<Ordonnance> findByPatientId(UUID patientId);
}
