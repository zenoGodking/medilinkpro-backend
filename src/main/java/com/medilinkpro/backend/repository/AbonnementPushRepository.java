package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.AbonnementPush;
import com.medilinkpro.backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AbonnementPushRepository extends JpaRepository<AbonnementPush, UUID> {

    List<AbonnementPush> findByUtilisateurId(UUID utilisateurId);

    Optional<AbonnementPush> findByEndpoint(String endpoint);

    /** Abonnements des comptes actifs et approuves d'un role (ex: toutes les infirmieres). */
    @Query("""
            SELECT a FROM AbonnementPush a, Utilisateur u
            WHERE a.utilisateurId = u.id AND u.role = :role AND u.actif = true
              AND u.statutCompte = com.medilinkpro.backend.enums.StatutCompte.APPROUVE
            """)
    List<AbonnementPush> findParRole(@Param("role") Role role);
}
