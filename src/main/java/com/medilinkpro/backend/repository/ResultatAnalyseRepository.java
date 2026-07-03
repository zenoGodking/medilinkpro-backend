package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.ResultatAnalyse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResultatAnalyseRepository extends JpaRepository<ResultatAnalyse, UUID> {

    List<ResultatAnalyse> findByDossierMedicalId(UUID dossierId);
}
