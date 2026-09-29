package com.medilinkpro.backend.repository.suivi;

import com.medilinkpro.backend.entity.suivi.MesureSante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MesureSanteRepository extends JpaRepository<MesureSante, UUID> {

    List<MesureSante> findByPatientIdOrderByDateMesureAsc(UUID patientId);
}
