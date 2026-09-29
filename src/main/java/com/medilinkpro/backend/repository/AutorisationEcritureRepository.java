package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.AutorisationEcriture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutorisationEcritureRepository extends JpaRepository<AutorisationEcriture, UUID> {

    boolean existsByPatientIdAndMedecinIdAndDateRevocationIsNull(UUID patientId, UUID medecinId);

    Optional<AutorisationEcriture> findFirstByPatientIdAndMedecinIdAndDateRevocationIsNull(UUID patientId, UUID medecinId);

    List<AutorisationEcriture> findByPatientIdAndDateRevocationIsNullOrderByDateAutorisationDesc(UUID patientId);

    List<AutorisationEcriture> findByMedecinIdAndDateRevocationIsNull(UUID medecinId);
}
