package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.DocumentMedical;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentMedicalRepository extends JpaRepository<DocumentMedical, UUID> {

    List<DocumentMedical> findByPatientIdOrderByDateAjoutDesc(UUID patientId);
}
