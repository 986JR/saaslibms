package com.saas.libms.audit.dto;

import com.saas.libms.audit.AuditAction;
import com.saas.libms.audit.AuditEntityType;
import com.saas.libms.audit.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponseDTO(
        UUID id,
        UUID institutionId,
        String actorId,
        String actorEmail,
        String actorRole,
        AuditAction action,
        AuditEntityType entityType,
        String entityPublicId,
        String metadata,
        LocalDateTime createdAt
) {
    public static AuditLogResponseDTO from(AuditLog log) {
        return new AuditLogResponseDTO(
                log.getId(),
                log.getInstitutionId(),
                log.getActorId(),
                log.getActorEmail(),
                log.getActorRole(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityPublicId(),
                log.getMetadata(),
                log.getCreatedAt()
        );
    }
}
