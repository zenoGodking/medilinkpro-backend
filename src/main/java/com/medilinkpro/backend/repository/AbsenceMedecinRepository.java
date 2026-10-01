package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.AbsenceMedecin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AbsenceMedecinRepository extends JpaRepository<AbsenceMedecin, UUID> {

    List<AbsenceMedecin> findByMedecinIdOrderByDateDebutAsc(UUID medecinId);

    /** Absences qui chevauchent l'intervalle [du, au]. */
    List<AbsenceMedecin> findByMedecinIdAndDateFinGreaterThanEqualAndDateDebutLessThanEqual(
            UUID medecinId, LocalDate du, LocalDate au);
}
