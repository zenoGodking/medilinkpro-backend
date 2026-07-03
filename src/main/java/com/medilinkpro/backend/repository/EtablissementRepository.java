package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.EtablissementSante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EtablissementRepository extends JpaRepository<EtablissementSante, UUID> {
}
