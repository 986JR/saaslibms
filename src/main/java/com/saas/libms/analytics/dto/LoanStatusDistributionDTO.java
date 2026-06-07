package com.saas.libms.analytics.dto;
/**
 * Loan count broken down by status.
 * Used for the pie/donut chart on the loans section.
 * Returned by GET /api/v1/system/analytics/loans/status-distribution
 */
public record LoanStatusDistributionDTO(
        long borrowed,    // status = BORROWED (active, not yet due)
        long late,        // status = LATE     (overdue)
        long returned     // status = RETURNED (completed)
) {}
