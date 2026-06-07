package com.saas.libms.analytics.dto;

import java.time.LocalDate;

/**
 * Generic one-data-point-per-day record.
 * Reused for multiple trend line charts:
 *   - loans created per day
 *   - daily active users
 *   - reservations created per day
 *
 * Returned by:
 *   GET /api/v1/system/analytics/loans/trend
 *   GET /api/v1/system/analytics/users/active-daily
 *   GET /api/v1/system/analytics/reservations/trend
 */
public record DailyCountDTO(
        LocalDate date,
        long count
) {}