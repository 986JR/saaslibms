package com.saas.libms.analytics.dto;
/**
 * One entry in the top-endpoints or slowest-endpoints table.
 * Returned by:
 *   GET /api/v1/system/analytics/traffic/top-endpoints
 *   GET /api/v1/system/analytics/traffic/slowest-endpoints
 */
public record EndpointMetricsDTO(
        String endpoint,          // e.g. "GET /api/v1/books/{id}"
        long callCount,           // total calls in the period
        double avgDurationMs      // average response time in ms
) {}
