package com.saas.libms.analytics.dto;
/**
 * Live traffic summary for the top-row KPI cards on the traffic page.
 * Returned by GET /api/v1/system/analytics/traffic/summary
 */
public record TrafficSummaryDTO(
        long requestsLastMinute,      // requests in the last 60 seconds
        long requestsLastHour,        // requests in the last 60 minutes
        long requestsToday,           // requests since midnight
        double avgResponseTimeMs,     // average across last 60 minutes
        long errorCountLastHour,      // 4xx + 5xx in the last 60 minutes
        double errorRatePercent       // (errors / total) * 100 for last hour
) {}
