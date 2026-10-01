package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.PlageDisponibilite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlageDisponibiliteRepository extends JpaRepository<PlageDisponibilite, UUID> {

    List<PlageDisponibilite> findByMedecinIdOrderByJourSemaineAscHeureDebutAsc(UUID medecinId);

    @Modifying
    @Query("DELETE FROM PlageDisponibilite p WHERE p.medecin.id = :medecinId")
    void supprimerParMedecin(@Param("medecinId") UUID medecinId);
}
