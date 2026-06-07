package com.saas.libms.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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
            @Param("actorId")       String actorId,
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

    // Audit action count per institution — used in institution activity ranking
// (higher count = more active institution)
    @Query("SELECT a.institutionId, COUNT(a) FROM AuditLog a GROUP BY a.institutionId")
    List<Object[]> countActionsPerInstitution();

    // Daily active users — distinct actors per day across the platform
// Returns Object[] where [0]=LocalDate, [1]=distinct actor count
    @Query("""
    SELECT CAST(a.createdAt AS LocalDate), COUNT(DISTINCT a.actorId)
    FROM AuditLog a
    WHERE a.createdAt >= :from
    GROUP BY CAST(a.createdAt AS LocalDate)
    ORDER BY CAST(a.createdAt AS LocalDate) ASC
    """)
    List<Object[]> countDailyActiveUsers(@Param("from") LocalDateTime from);

    // Top active users by action count
// Returns Object[] where [0]=actorEmail, [1]=actorRole, [2]=institutionId, [3]=count
    @Query("""
    SELECT a.actorEmail, a.actorRole, a.institutionId, COUNT(a)
    FROM AuditLog a
    GROUP BY a.actorEmail, a.actorRole, a.institutionId
    ORDER BY COUNT(a) DESC
    """)
    List<Object[]> findTopActiveUsers(Pageable pageable);
}
