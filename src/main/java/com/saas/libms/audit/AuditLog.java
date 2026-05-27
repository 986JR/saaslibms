package com.saas.libms.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table( name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_institution",  columnList = "institution_id"),
                @Index(name = "idx_audit_actor",        columnList = "actor_id"),
                @Index(name = "idx_audit_entity",       columnList = "entity_type, entity_public_id"),
                @Index(name = "idx_audit_created_at",   columnList = "created_at"),
                @Index(name = "idx_audit_action",       columnList = "action")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "instituiton_id", nullable = false)
    private UUID institutionId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "actor_role", nullable = false)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 60)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private AuditEntityType entityType;

    @Column(name = "entity_public_id", length = 30)
    private String entityPublicId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
