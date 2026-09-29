package com.medilinkpro.backend.repository.suivi;

import com.medilinkpro.backend.entity.suivi.RappelMedicament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RappelMedicamentRepository extends JpaRepository<RappelMedicament, UUID> {

    List<RappelMedicament> findByPatientIdOrderByMedicamentAsc(UUID patientId);

    List<RappelMedicament> findByActifTrue();
}
