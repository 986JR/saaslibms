package com.saas.libms.analytics.dto;
/**
 * HTTP response status code distribution.
 * Returned by GET /api/v1/system/analytics/traffic/error-rates
 */
public record ErrorRateDTO(
        long count2xx,           // successful responses
        long count4xx,           // client errors (bad request, not found, rate limited)
        long count5xx,           // server errors
        long totalRequests,
        double errorRatePercent  // (4xx + 5xx) / total * 100
) {}
