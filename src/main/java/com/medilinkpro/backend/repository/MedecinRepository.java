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

    List<Medecin> findByEtablissement_Directeur_Id(UUID directeurId);

    /**
     * Recherche de medecins par specialite, ville et/ou quartier (filtres optionnels,
     * combinables). Les medecins de la meme ville/quartier remontent en priorite.
     * CAST(... AS string) : sans lui, PostgreSQL type un parametre null en bytea et
     * LOWER(bytea) echoue (erreur 500 des qu'un filtre est vide).
     */
    @Query("""
            SELECT m FROM Medecin m
            WHERE (:specialite IS NULL OR LOWER(m.specialite) LIKE LOWER(CONCAT('%', CAST(:specialite AS string), '%')))
              AND (:ville IS NULL OR LOWER(m.ville) LIKE LOWER(CONCAT('%', CAST(:ville AS string), '%')))
              AND (:quartier IS NULL OR LOWER(m.quartier) LIKE LOWER(CONCAT('%', CAST(:quartier AS string), '%')))
            ORDER BY m.nom ASC
            """)
    List<Medecin> rechercher(@Param("specialite") String specialite,
                              @Param("ville") String ville,
                              @Param("quartier") String quartier);
}
