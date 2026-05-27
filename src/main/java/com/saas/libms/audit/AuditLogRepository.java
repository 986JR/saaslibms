package com.saas.libms.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.institutionId = :institutionId
          AND (:action       IS NULL OR a.action     = :action)
          AND (:entityType   IS NULL OR a.entityType = :entityType)
          AND (:actorId      IS NULL OR a.actorId    = :actorId)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> findAllByInstitutionId(
            @Param("institutionId") UUID institutionId,
            @Param("action")        AuditAction action,
            @Param("entityType")    AuditEntityType entityType,
            @Param("actorId")       UUID actorId,
            Pageable pageable
    );

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:action       IS NULL OR a.action          = :action)
          AND (:entityType   IS NULL OR a.entityType      = :entityType)
          AND (:institutionId IS NULL OR a.institutionId  = :institutionId)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> findAllForSystem(
            @Param("action")        AuditAction action,
            @Param("entityType")    AuditEntityType entityType,
            @Param("institutionId") UUID institutionId,
            Pageable pageable
    );
}
