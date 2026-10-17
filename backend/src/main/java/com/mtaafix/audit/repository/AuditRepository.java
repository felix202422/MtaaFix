package com.mtaafix.audit.repository;

import com.mtaafix.audit.domain.AuditLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findByEntityId(String entityId);

    /** P22: filtered audit search (FR-10.x). */
    @Query("""
            select a from AuditLog a
            where (:action is null or a.action = :action)
              and (:actorId is null or a.actorId = :actorId)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:outcome is null or a.outcome = :outcome)
              and (cast(:from as timestamp) is null or a.createdAt >= :from)
              and (cast(:to as timestamp) is null or a.createdAt <= :to)
            order by a.createdAt desc
            """)
    Page<AuditLog> search(@Param("action") String action, @Param("actorId") String actorId,
            @Param("entityType") String entityType, @Param("entityId") String entityId,
            @Param("outcome") String outcome, @Param("from") Instant from,
            @Param("to") Instant to, Pageable pageable);

    /** P22: filtered audit search scoped to a set of entity ids (org scoping). */
    @Query("""
            select a from AuditLog a
            where a.entityId in (:entityIds)
              and (:action is null or a.action = :action)
              and (:actorId is null or a.actorId = :actorId)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:outcome is null or a.outcome = :outcome)
              and (cast(:from as timestamp) is null or a.createdAt >= :from)
              and (cast(:to as timestamp) is null or a.createdAt <= :to)
            order by a.createdAt desc
            """)
    Page<AuditLog> searchInEntities(@Param("entityIds") List<String> entityIds,
            @Param("action") String action, @Param("actorId") String actorId,
            @Param("entityType") String entityType, @Param("entityId") String entityId,
            @Param("outcome") String outcome,
            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
