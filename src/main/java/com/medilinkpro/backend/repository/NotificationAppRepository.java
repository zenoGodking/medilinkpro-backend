package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.NotificationApp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationAppRepository extends JpaRepository<NotificationApp, UUID> {

    List<NotificationApp> findByDestinataireIdOrderByDateCreationDesc(UUID destinataireId, Pageable page);

    long countByDestinataireIdAndLueFalse(UUID destinataireId);

    @Modifying
    @Query("UPDATE NotificationApp n SET n.lue = true WHERE n.destinataireId = :destinataireId AND n.lue = false")
    int marquerToutesLues(@Param("destinataireId") UUID destinataireId);
}
