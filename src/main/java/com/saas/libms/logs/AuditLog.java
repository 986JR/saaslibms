package com.saas.libms.logs;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_tenant", columnList = "tenant_id"),
                @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_audit_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Which library/tenant performed the action
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /*
     * User who performed the action
     */
    @Column(name = "user_id")
    private UUID userId;

    /*
     * ADMIN, LIBRARIAN, MEMBER, etc
     */
    @Column(name = "user_role")
    private String userRole;

    /*
     * BOOK_CREATED, MEMBER_UPDATED, LOGIN_SUCCESS
     */
    @Column(nullable = false)
    private String action;

    /*
     * BOOK, MEMBER, LOAN, USER
     */
    @Column(name = "entity_type")
    private String entityType;

    /*
     * ID of affected entity
     */
    @Column(name = "entity_id")
    private String entityId;

    /*
     * Human-readable description
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /*
     * Previous values (JSON)
     */
    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;

    /*
     * New values (JSON)
     */
    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;

    /*
     * SUCCESS / FAILED

    @Enumerated(EnumType.STRING)
    private AuditStatus status;*/

    /*
     * Request metadata
     */
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /*
     * Optional request tracing
     */
    @Column(name = "trace_id")
    private UUID traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
