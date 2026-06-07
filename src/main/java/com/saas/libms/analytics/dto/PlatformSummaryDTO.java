package com.saas.libms.analytics.dto;
/**
        * Top-row KPI cards on the main dashboard.
 * All values are simple counts across the entire platform.
 * Returned by GET /api/v1/system/analytics/summary
 */
public record PlatformSummaryDTO(
        long totalInstitutions,
        long activeInstitutions,
        long suspendedInstitutions,
        long totalUsers,
        long activeUsers,
        long totalBooks,
        long totalMembers,
        long totalLoans,
        long activeLoans,        // status = BORROWED or LATE
        long overdueLoans,       // status = LATE
        long totalReservations,
        long pendingReservations // status = PENDING
) {}
