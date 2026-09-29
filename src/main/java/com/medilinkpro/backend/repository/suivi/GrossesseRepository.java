package com.medilinkpro.backend.repository.suivi;

import com.medilinkpro.backend.entity.suivi.Grossesse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GrossesseRepository extends JpaRepository<Grossesse, UUID> {

    List<Grossesse> findByPatientIdOrderByDateDernieresReglesDesc(UUID patientId);
}
