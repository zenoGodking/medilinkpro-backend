package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.Infirmier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InfirmierRepository extends JpaRepository<Infirmier, UUID> {

    /**
     * Infirmieres pouvant recevoir une alerte : compte actif et approuve, position recente
     * (application ouverte) et aucune intervention en cours.
     */
    @Query("""
            SELECT i FROM Infirmier i
            WHERE i.actif = true
              AND i.statutCompte = com.medilinkpro.backend.enums.StatutCompte.APPROUVE
              AND i.latitude IS NOT NULL AND i.longitude IS NOT NULL
              AND i.datePosition >= :depuis
              AND NOT EXISTS (
                  SELECT a FROM AlerteSoinDomicile a
                  WHERE a.infirmier = i AND a.statut = com.medilinkpro.backend.enums.StatutAlerte.REPONDUE)
            """)
    List<Infirmier> findDisponiblesLocalisees(@Param("depuis") LocalDateTime depuis);

    List<Infirmier> findByEtablissementIdOrderByNomAsc(UUID etablissementId);

    List<Infirmier> findByEtablissement_Directeur_Id(UUID directeurId);
}
