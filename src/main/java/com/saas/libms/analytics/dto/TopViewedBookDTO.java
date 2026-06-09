package com.saas.libms.analytics.dto;
import java.util.UUID;

/**
 * One entry in the most-viewed books ranking.
 * Returned by GET /api/v1/system/analytics/books/top-viewed
 */
public record TopViewedBookDTO(
        String publicId,
        String title,
        UUID institutionId,    // raw UUID — frontend resolves name via institution activity data
        long viewCount
) {}