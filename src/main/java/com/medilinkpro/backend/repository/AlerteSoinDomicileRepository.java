package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.AlerteSoinDomicile;
import com.medilinkpro.backend.entity.Infirmier;
import com.medilinkpro.backend.enums.StatutAlerte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AlerteSoinDomicileRepository extends JpaRepository<AlerteSoinDomicile, UUID> {

    List<AlerteSoinDomicile> findByStatutOrderByDateCreationDesc(StatutAlerte statut);

    List<AlerteSoinDomicile> findByPatientIdOrderByDateCreationDesc(UUID patientId);

    /**
     * Assigne atomiquement l'alerte a une infirmiere : la mise a jour ne s'applique
     * que si l'alerte est encore EN_ATTENTE, ce qui evite qu'une deuxieme infirmiere
     * "prenne" la meme alerte en cas de reponse quasi simultanee (race condition).
     * Retourne le nombre de lignes affectees : 0 si l'alerte etait deja prise/annulee.
     */
    @Modifying
    @Query("""
            UPDATE AlerteSoinDomicile a
            SET a.statut = com.medilinkpro.backend.enums.StatutAlerte.REPONDUE,
                a.infirmier = :infirmier,
                a.dateReponse = :dateReponse
            WHERE a.id = :alerteId AND a.statut = com.medilinkpro.backend.enums.StatutAlerte.EN_ATTENTE
            """)
    int repondreSiDisponible(@Param("alerteId") UUID alerteId,
                              @Param("infirmier") Infirmier infirmier,
                              @Param("dateReponse") LocalDateTime dateReponse);

    List<AlerteSoinDomicile> findByInfirmierIdAndStatutOrderByDateReponseDesc(UUID infirmierId, StatutAlerte statut);

    /**
     * Retire l'infirmiere responsable et remet l'alerte EN_ATTENTE, uniquement si
     * elle est bien la responsable actuelle et que l'intervention n'est pas deja
     * terminee/annulee (protection contre les mises a jour concurrentes).
     * Retourne le nombre de lignes affectees : 0 si la retractation n'est plus possible.
     */
    @Modifying
    @Query("""
            UPDATE AlerteSoinDomicile a
            SET a.statut = com.medilinkpro.backend.enums.StatutAlerte.EN_ATTENTE,
                a.infirmier = null,
                a.dateReponse = null
            WHERE a.id = :alerteId AND a.infirmier.id = :infirmierId
              AND a.statut = com.medilinkpro.backend.enums.StatutAlerte.REPONDUE
            """)
    int retracterSiResponsable(@Param("alerteId") UUID alerteId, @Param("infirmierId") UUID infirmierId);

    /**
     * Enregistre la note du patient et cloture l'alerte, uniquement si elle appartient
     * bien a ce patient et qu'une infirmiere est effectivement intervenue (REPONDUE).
     * Retourne le nombre de lignes affectees : 0 si la notation n'est plus possible.
     */
    @Modifying
    @Query("""
            UPDATE AlerteSoinDomicile a
            SET a.statut = com.medilinkpro.backend.enums.StatutAlerte.TERMINEE,
                a.note = :note,
                a.commentaire = :commentaire,
                a.dateNotation = :dateNotation
            WHERE a.id = :alerteId AND a.patient.id = :patientId
              AND a.statut = com.medilinkpro.backend.enums.StatutAlerte.REPONDUE
            """)
    int noterSiEligible(@Param("alerteId") UUID alerteId,
                         @Param("patientId") UUID patientId,
                         @Param("note") Integer note,
                         @Param("commentaire") String commentaire,
                         @Param("dateNotation") LocalDateTime dateNotation);

    @Query("SELECT AVG(a.note) FROM AlerteSoinDomicile a WHERE a.infirmier.id = :infirmierId AND a.note IS NOT NULL")
    Double moyenneNoteInfirmier(@Param("infirmierId") UUID infirmierId);

    long countByInfirmierIdAndNoteIsNotNull(UUID infirmierId);
}
