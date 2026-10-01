package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.CodeReinitialisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CodeReinitialisationRepository extends JpaRepository<CodeReinitialisation, UUID> {

    List<CodeReinitialisation> findByUtilisateurIdAndUtiliseFalse(UUID utilisateurId);
}
