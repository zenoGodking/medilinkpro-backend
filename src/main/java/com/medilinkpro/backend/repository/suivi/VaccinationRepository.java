package com.medilinkpro.backend.repository.suivi;

import com.medilinkpro.backend.entity.suivi.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VaccinationRepository extends JpaRepository<Vaccination, UUID> {

    List<Vaccination> findByPatientIdOrderByDateVaccinationDesc(UUID patientId);
}
