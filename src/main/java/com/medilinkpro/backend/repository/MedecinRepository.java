package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.Medecin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedecinRepository extends JpaRepository<Medecin, UUID> {

    Optional<Medecin> findByEmail(String email);

    List<Medecin> findBySpecialiteIgnoreCaseContaining(String specialite);

    /**
     * Recherche de medecins par specialite, tries par distance approximative
     * (formule euclidienne simplifiee, suffisante pour un rayon local).
     * Pour une vraie recherche geospatiale en production, utiliser PostGIS (ST_Distance).
     */
    @Query("""
            SELECT m FROM Medecin m
            WHERE (:specialite IS NULL OR LOWER(m.specialite) LIKE LOWER(CONCAT('%', :specialite, '%')))
            AND m.latitude IS NOT NULL AND m.longitude IS NOT NULL
            ORDER BY (POWER(m.latitude - :lat, 2) + POWER(m.longitude - :lng, 2)) ASC
            """)
    List<Medecin> rechercherParSpecialiteEtLocalisation(
            @Param("specialite") String specialite,
            @Param("lat") Double lat,
            @Param("lng") Double lng);
}
