package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.DossierMedical;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DossierMedicalRepository extends JpaRepository<DossierMedical, UUID> {

    Optional<DossierMedical> findByPatientId(UUID patientId);
}
