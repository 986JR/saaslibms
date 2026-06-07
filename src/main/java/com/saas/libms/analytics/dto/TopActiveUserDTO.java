package com.saas.libms.analytics.dto;

/**
 * One entry in the most-active-users ranking table.
 * Activity is measured by audit log action count.
 * Returned by GET /api/v1/system/analytics/users/top-active
 */
public record TopActiveUserDTO(
        String actorEmail,
        String actorRole,
        String institutionName,
        long actionCount
) {}
