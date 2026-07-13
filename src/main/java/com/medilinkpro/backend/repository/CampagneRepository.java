package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.Campagne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CampagneRepository extends JpaRepository<Campagne, UUID> {

    List<Campagne> findByEtablissementIdOrderByDateDebutDesc(UUID etablissementId);

    /** Campagnes actuellement actives pour un etablissement donne (visite de sa fiche publique). */
    @Query("""
            SELECT c FROM Campagne c
            WHERE c.etablissement.id = :etablissementId AND c.actif = true
              AND c.dateDebut <= :aujourdhui AND (c.dateFin IS NULL OR c.dateFin >= :aujourdhui)
            ORDER BY c.dateDebut DESC
            """)
    List<Campagne> findActivesByEtablissement(@Param("etablissementId") UUID etablissementId,
                                               @Param("aujourdhui") LocalDate aujourdhui);

    /** Toutes les campagnes actives, tous etablissements confondus (fil de notifications publiques). */
    @Query("""
            SELECT c FROM Campagne c
            WHERE c.actif = true
              AND c.dateDebut <= :aujourdhui AND (c.dateFin IS NULL OR c.dateFin >= :aujourdhui)
            ORDER BY c.dateDebut DESC
            """)
    List<Campagne> findActives(@Param("aujourdhui") LocalDate aujourdhui);
}
