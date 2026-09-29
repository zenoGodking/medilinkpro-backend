package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.AccesCarnet;
import com.medilinkpro.backend.enums.TypeAccesCarnet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AccesCarnetRepository extends JpaRepository<AccesCarnet, UUID> {

    boolean existsByPatientIdAndUtilisateurIdAndTypeAccesAndDateAccesAfter(
            UUID patientId, UUID utilisateurId, TypeAccesCarnet typeAcces, LocalDateTime apres);

    List<AccesCarnet> findByPatientIdOrderByDateAccesDesc(UUID patientId, Pageable pageable);
}
