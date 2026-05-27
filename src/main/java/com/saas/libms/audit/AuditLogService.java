package com.saas.libms.audit;

import com.saas.libms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {
    private final   AuditLogRepository auditLogRepository;

    public void log(
            CustomUserDetails actor,
            AuditAction action,
            AuditEntityType entityType,
            String entityPublicId,
            String metadata
    ) {
        try {
            AuditLog entry = AuditLog.builder()
                    .institutionId(actor.getUser().getInstitution().getId())
                    .actorId(actor.getUser().getPublicId())
                    .actorEmail(actor.getUser().getEmail())
                    .actorRole(actor.getUser().getRole().name())
                    .action(action)
                    .entityType(entityType)
                    .entityPublicId(entityPublicId)
                    .metadata(metadata)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: action={}, entity={}/{}, actor={} — {}",
                    action, entityType, entityPublicId,
                    actor != null ? actor.getUsername() : "unknown",
                    e.getMessage(), e);
        }
    }
}
