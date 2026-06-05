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

import com.saas.libms.institution.InstitutionRepository;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final InstitutionRepository institutionRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getAuditLogs(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String institutionPublicId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
            ) {

        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);

        // Parse optional enum filters. Invalid values are treated as "no filter"
        // rather than throwing an exception — cleaner UX for the frontend.
        AuditAction actionFilter = parseAction(action);
        AuditEntityType entityTypeFilter = parseEntityType(entityType);

        Page<AuditLogResponseDTO> result;

        if (currentUser.getUser().getRole() == com.saas.libms.user.UserRole.SYSTEM) {
            UUID filterInstId = null;
            if (institutionPublicId != null && !institutionPublicId.isBlank()) {
                filterInstId = institutionRepository.findByPublicId(institutionPublicId)
                        .map(com.saas.libms.institution.Institution::getId)
                        .orElse(null);
            }
            result = auditLogRepository
                    .findAllForSystem(actionFilter, entityTypeFilter, filterInstId, pageable)
                    .map(AuditLogResponseDTO::from);
        } else {
            UUID institutionId = currentUser.getUser().getInstitution().getId();
            result = auditLogRepository
                    .findAllByInstitutionId(institutionId, actionFilter, entityTypeFilter, actorId, pageable)
                    .map(AuditLogResponseDTO::from);
        }

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