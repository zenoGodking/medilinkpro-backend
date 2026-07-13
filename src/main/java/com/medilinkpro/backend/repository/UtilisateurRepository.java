package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.Utilisateur;
import com.medilinkpro.backend.enums.Role;
import com.medilinkpro.backend.enums.StatutCompte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Utilisateur> findByStatutCompte(StatutCompte statutCompte);

    List<Utilisateur> findByStatutCompteAndRole(StatutCompte statutCompte, Role role);

    List<Utilisateur> findByRole(Role role);
}
