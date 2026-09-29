package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.EtablissementSante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EtablissementRepository extends JpaRepository<EtablissementSante, UUID> {

    /** Incremente atomiquement le compteur de visites publiques (evite les pertes en cas d'acces concurrents). */
    @Modifying
    @Query("UPDATE EtablissementSante e SET e.nombreVisites = e.nombreVisites + 1 WHERE e.id = :id")
    int incrementerVisites(@Param("id") UUID id);

    java.util.List<EtablissementSante> findByDirecteurId(java.util.UUID directeurId);
}
