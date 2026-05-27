package com.saas.libms.audit;

import com.saas.libms.audit.dto.AuditLogResponseDTO;
import com.saas.libms.common.ApiResponse;
import com.saas.libms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getAuditLogs(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actorId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
            ) {

        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);

        UUID institutionId = currentUser.getUser().getInstitution().getId();

        // Parse optional enum filters. Invalid values are treated as "no filter"
        // rather than throwing an exception — cleaner UX for the frontend.
        AuditAction actionFilter = parseAction(action);
        AuditEntityType entityTypeFilter = parseEntityType(entityType);

        Page<AuditLogResponseDTO> result = auditLogRepository
                .findAllByInstitutionId(institutionId, actionFilter, entityTypeFilter, actorId, pageable)
                .map(AuditLogResponseDTO::from);

        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", result));

    }

    private AuditAction parseAction(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return AuditAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private AuditEntityType parseEntityType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return AuditEntityType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}