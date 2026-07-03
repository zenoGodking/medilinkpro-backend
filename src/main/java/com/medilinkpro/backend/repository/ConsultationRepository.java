package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    List<Consultation> findByDossierMedicalId(UUID dossierId);

    List<Consultation> findByMedecinId(UUID medecinId);

    List<Consultation> findByPatientId(UUID patientId);
}
