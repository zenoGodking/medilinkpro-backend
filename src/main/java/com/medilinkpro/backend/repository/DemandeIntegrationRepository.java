package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.DemandeIntegration;
import com.medilinkpro.backend.enums.StatutDemandeIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandeIntegrationRepository extends JpaRepository<DemandeIntegration, UUID> {

    List<DemandeIntegration> findByMedecinIdOrderByDateCreationDesc(UUID medecinId);

    List<DemandeIntegration> findByEtablissementIdOrderByDateCreationDesc(UUID etablissementId);

    boolean existsByMedecinIdAndEtablissementIdAndStatut(
            UUID medecinId, UUID etablissementId, StatutDemandeIntegration statut);

    List<DemandeIntegration> findByInfirmierIdOrderByDateCreationDesc(UUID infirmierId);

    boolean existsByInfirmierIdAndEtablissementIdAndStatut(
            UUID infirmierId, UUID etablissementId, StatutDemandeIntegration statut);

    List<DemandeIntegration> findByStatutOrderByDateCreationDesc(StatutDemandeIntegration statut);

    /** Demandes concernant les etablissements dont ce directeur est responsable. */
    List<DemandeIntegration> findByEtablissement_Directeur_IdAndStatutOrderByDateCreationDesc(
            UUID directeurId, StatutDemandeIntegration statut);
}
