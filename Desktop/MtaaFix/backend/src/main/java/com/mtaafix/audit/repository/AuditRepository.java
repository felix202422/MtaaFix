package com.mtaafix.audit.repository;

import com.mtaafix.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findByEntityId(String entityId);
}
