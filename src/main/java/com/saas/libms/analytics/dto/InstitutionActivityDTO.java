package com.saas.libms.analytics.dto;

import java.time.LocalDateTime;

/**
 * Activity summary for a single institution.
 * Used in the institution ranking table.
 * Returned by GET /api/v1/system/analytics/institutions/activity
 */
public record InstitutionActivityDTO(
        String publicId,
        String name,
        String status,
        long userCount,
        long bookCount,
        long memberCount,
        long loanCount,
        long reservationCount,
        long auditActionCount,   // total staff actions logged — proxy for general activity
        LocalDateTime createdAt
) {}
