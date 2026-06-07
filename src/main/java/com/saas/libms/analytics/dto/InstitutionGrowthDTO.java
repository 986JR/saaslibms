package com.saas.libms.analytics.dto;

import java.time.LocalDate;
/**
 * One data point for the institution growth line chart.
 * Returned by GET /api/v1/system/analytics/institutions/growth
 */
public record InstitutionGrowthDTO(
        LocalDate date,
        long count            // institutions registered on this date
) {}
