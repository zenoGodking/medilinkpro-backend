package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.NotificationSms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationSmsRepository extends JpaRepository<NotificationSms, UUID> {

    List<NotificationSms> findByPatientId(UUID patientId);
}
