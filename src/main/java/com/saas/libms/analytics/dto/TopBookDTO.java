package com.saas.libms.analytics.dto;

import java.util.UUID;

/**
 * One entry in a top-books ranking list.
 * Used for both top-borrowed and top-reserved charts.
 * Returned by:
 *   GET /api/v1/system/analytics/books/top-borrowed
 *   GET /api/v1/system/analytics/books/top-reserved
 *   GET /api/v1/system/analytics/books/least-borrowed
 */
public record TopBookDTO(
        String publicId,
        String title,
        String institutionName,
        long count              // loan count OR reservation count depending on endpoint
) {}
