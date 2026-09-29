package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByJetonCarteUrgence(String jetonCarteUrgence);

    /** Patients disposant d'une empreinte faciale, pour la recherche par reconnaissance faciale. */
    @Query("select p from Patient p where p.descripteurFacial is not null and p.actif = true")
    List<Patient> findAllAvecEmpreinteFaciale();
}
