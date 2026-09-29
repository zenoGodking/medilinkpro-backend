package com.medilinkpro.backend.repository;

import com.medilinkpro.backend.entity.AccesUrgenceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccesUrgenceLogRepository extends JpaRepository<AccesUrgenceLog, UUID> {
}
