package com.saas.libms.analytics.dto;

import java.util.Map;

/**
 * PostgreSQL and connection pool metrics.
 * Returned by GET /api/v1/system/analytics/infrastructure/database
 */
public record DatabaseMetricsDTO(
        //  Database size
        long databaseSizeBytes,        // total DB size from pg_database_size()
        String databaseSizeFormatted,  // human-readable: "142 MB"

        // HikariCP connection pool
        int activeConnections,         // connections currently executing queries
        int idleConnections,           // connections waiting for work
        int pendingConnections,        // threads waiting for a connection (> 0 = pressure)
        int maxConnections,            // pool maximum (spring.datasource.hikari.maximum-pool-size)

        // Table row counts
        // Key = table name, Value = approximate row count from pg_stat_user_tables
        // "approximate" because PostgreSQL uses statistics — exact for analytics purposes
        Map<String, Long> tableRowCounts
) {}
