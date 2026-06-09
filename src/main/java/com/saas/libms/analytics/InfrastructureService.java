package com.saas.libms.analytics;

import com.saas.libms.analytics.dto.DatabaseMetricsDTO;
import com.saas.libms.analytics.dto.JvmMetricsDTO;
import com.saas.libms.analytics.dto.RedisMetricsDTO;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfrastructureService {

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redisTemplate;
    private final EntityManager entityManager;

    public JvmMetricsDTO getJvmMetrics() {
        // Heap
        long heapUsed = getGaugeLong("jvm.memory.used", "area", "heap");
        long heapMax  = getGaugeLong("jvm.memory.max",  "area", "heap");
        double heapPercent = heapMax > 0
                ? Math.round((heapUsed * 100.0 / heapMax) * 100.0) / 100.0
                : 0.0;

        // Non-heap (Metaspace + code cache)
        long nonHeapUsed = getGaugeLong("jvm.memory.used", "area", "nonheap");

        // Threads
        int liveThreads   = (int) getGaugeDouble("jvm.threads.live");
        int daemonThreads = (int) getGaugeDouble("jvm.threads.daemon");
        int peakThreads   = (int) getGaugeDouble("jvm.threads.peak");

        // CPU — Micrometer returns 0.0–1.0, we convert to 0.0–100.0
        double cpuRaw = getGaugeDouble("system.cpu.usage");
        double cpuPercent = Math.round(cpuRaw * 100.0 * 100.0) / 100.0;
        int processors = (int) getGaugeDouble("system.cpu.count");

        // Uptime
        long uptimeSeconds = (long) getGaugeDouble("process.uptime");
        String uptimeFormatted = formatUptime(uptimeSeconds);

        return new JvmMetricsDTO(
                heapUsed, heapMax, heapPercent,
                nonHeapUsed,
                liveThreads, daemonThreads, peakThreads,
                cpuPercent, processors,
                uptimeSeconds, uptimeFormatted
        );
    }

    // ── 2. Database Metrics

    /**
     * Reads PostgreSQL DB size and table row counts via native SQL.
     * Reads HikariCP connection pool state via Micrometer gauges.
     *
     * Native SQL used (PostgreSQL-specific):
     *   pg_database_size(current_database())  — total DB size in bytes
     *   pg_stat_user_tables                   — approximate row counts per table
     *
     * HikariCP Micrometer gauge names:
     *   hikaricp.connections.active
     *   hikaricp.connections.idle
     *   hikaricp.connections.pending
     *   hikaricp.connections.max
     * These are auto-registered when micrometer-registry-prometheus is on the classpath
     * and spring.datasource.hikari.pool-name is set in application.properties.
     */
    @Transactional(readOnly = true)
    public DatabaseMetricsDTO getDatabaseMetrics() {
        // DB size
        long dbSizeBytes = getDatabaseSizeBytes();
        String dbSizeFormatted = formatBytes(dbSizeBytes);

        // Table row counts — from PostgreSQL statistics
        Map<String, Long> tableRowCounts = getTableRowCounts();

        // HikariCP pool state from Micrometer
        // Tag "pool" value must match spring.datasource.hikari.pool-name
        String poolName = "LibmsHikariPool";
        int activeConnections  = (int) getHikariGauge("hikaricp.connections.active",  poolName);
        int idleConnections    = (int) getHikariGauge("hikaricp.connections.idle",    poolName);
        int pendingConnections = (int) getHikariGauge("hikaricp.connections.pending", poolName);
        int maxConnections     = (int) getHikariGauge("hikaricp.connections.max",     poolName);

        return new DatabaseMetricsDTO(
                dbSizeBytes, dbSizeFormatted,
                activeConnections, idleConnections, pendingConnections, maxConnections,
                tableRowCounts
        );
    }

    // ── 3. Redis Metrics

    /**
     * Reads Redis metrics via the INFO command.
     *
     * Redis INFO returns a multi-section text response. We execute it
     * via RedisConnection.serverCommands().info() which returns all sections.
     *
     * Sections we parse:
     *   # Memory   → used_memory, maxmemory
     *   # Stats    → keyspace_hits, keyspace_misses, evicted_keys
     *   # Clients  → connected_clients
     *   # Server   → redis_version, uptime_in_seconds
     *   # Keyspace → db0:keys=N  (total key count)
     *
     * The rl:* key count and cache key count are read separately using
     * StringRedisTemplate.keys() pattern scan — same as Phase 22-A.
     */
    public RedisMetricsDTO getRedisMetrics() {
        try {
            // Execute Redis INFO — returns all sections as a Properties-like string
            Properties info = redisTemplate.execute((RedisConnection connection) ->
                    connection.serverCommands().info());

            if (info == null) {
                log.warn("Redis INFO returned null — Redis may be unreachable");
                return emptyRedisMetrics();
            }

            // ── Memory
            long usedMemoryBytes = parseLong(info, "used_memory");
            long maxMemoryBytes  = parseLong(info, "maxmemory");
            String usedMemoryFormatted = formatBytes(usedMemoryBytes);
            double memoryUsagePercent = maxMemoryBytes > 0
                    ? Math.round((usedMemoryBytes * 100.0 / maxMemoryBytes) * 100.0) / 100.0
                    : 0.0;

            // ── Hit/Miss
            long keyspaceHits   = parseLong(info, "keyspace_hits");
            long keyspaceMisses = parseLong(info, "keyspace_misses");
            long totalLookups   = keyspaceHits + keyspaceMisses;
            double hitRatePercent = totalLookups > 0
                    ? Math.round((keyspaceHits * 100.0 / totalLookups) * 100.0) / 100.0
                    : 0.0;

            // ── Evictions
            long evictedKeys = parseLong(info, "evicted_keys");

            // ── Connections
            int connectedClients = (int) parseLong(info, "connected_clients");

            // ── Server info
            String redisVersion  = info.getProperty("redis_version", "unknown");
            long uptimeSeconds   = parseLong(info, "uptime_in_seconds");

            // ── Key counts by category
            long totalKeys     = countRedisKeys("*");
            long cacheKeys     = countRedisKeys("books::*")
                    + countRedisKeys("authors::*")
                    + countRedisKeys("categories::*")
                    + countRedisKeys("members::*");
            long rateLimitKeys = countRedisKeys("rl:*");

            return new RedisMetricsDTO(
                    usedMemoryBytes, maxMemoryBytes, usedMemoryFormatted, memoryUsagePercent,
                    keyspaceHits, keyspaceMisses, hitRatePercent,
                    totalKeys, cacheKeys, rateLimitKeys, evictedKeys,
                    connectedClients,
                    redisVersion, uptimeSeconds
            );

        } catch (Exception e) {
            log.warn("Failed to read Redis metrics: {}", e.getMessage());
            return emptyRedisMetrics();
        }
    }

    // ── Private Helpers — JVM

    /**
     * Read a Micrometer gauge that is filtered by a single tag key-value pair.
     * Returns the gauge value as a long (most memory/thread values are integral).
     */
    private long getGaugeLong(String metricName, String tagKey, String tagValue) {
        return (long) getGaugeDouble(metricName, tagKey, tagValue);
    }

    private double getGaugeDouble(String metricName, String tagKey, String tagValue) {
        try {
            return Search.in(meterRegistry)
                    .name(metricName)
                    .tag(tagKey, tagValue)
                    .gauges()
                    .stream()
                    .mapToDouble(g -> g.value())
                    .sum();
        } catch (Exception e) {
            log.debug("Metric not found: {} [{}={}]", metricName, tagKey, tagValue);
            return 0.0;
        }
    }

    /** Read a Micrometer gauge with no tag filter. */
    private double getGaugeDouble(String metricName) {
        try {
            return Search.in(meterRegistry)
                    .name(metricName)
                    .gauges()
                    .stream()
                    .mapToDouble(g -> g.value())
                    .findFirst()
                    .orElse(0.0);
        } catch (Exception e) {
            log.debug("Metric not found: {}", metricName);
            return 0.0;
        }
    }

    /** Read a HikariCP pool gauge — uses the "pool" tag. */
    private double getHikariGauge(String metricName, String poolName) {
        return getGaugeDouble(metricName, "pool", poolName);
    }

    /**
     * Format uptime seconds into a human-readable string.
     * Example: 90070 seconds → "1d 1h 1m"
     */
    private String formatUptime(long totalSeconds) {
        long days    = totalSeconds / 86400;
        long hours   = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600)  / 60;

        if (days > 0)  return days  + "d " + hours   + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    // ── Private Helpers — Database

    /**
     * PostgreSQL pg_database_size() — total size of the current database.
     * Returns bytes as a long.
     */
    private long getDatabaseSizeBytes() {
        try {
            Query query = entityManager.createNativeQuery(
                    "SELECT pg_database_size(current_database())");
            Object result = query.getSingleResult();
            return ((Number) result).longValue();
        } catch (Exception e) {
            log.warn("Could not read database size: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Reads approximate row counts for all application tables from
     * pg_stat_user_tables. These are statistics-based estimates maintained
     * by PostgreSQL's autovacuum process — accurate enough for monitoring.
     *
     * Returns only our application tables (excludes Spring Boot internal ones).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> getTableRowCounts() {
        Map<String, Long> result = new LinkedHashMap<>();
        try {
            Query query = entityManager.createNativeQuery("""
                    SELECT relname, n_live_tup
                    FROM pg_stat_user_tables
                    WHERE schemaname = 'public'
                    ORDER BY n_live_tup DESC
                    """);

            // Tables to include — our application tables only
            Set<String> appTables = Set.of(
                    "institutions", "institution_verifications",
                    "users", "refresh_sessions", "blacklisted_tokens",
                    "password_reset_tokens",
                    "books", "authors", "book_authors", "categories",
                    "members", "loans", "reservations", "audit_logs",
                    "api_request_logs", "book_view_events"
            );

            List<Object[]> rows = query.getResultList();
            for (Object[] row : rows) {
                String tableName = (String) row[0];
                if (appTables.contains(tableName)) {
                    long rowCount = ((Number) row[1]).longValue();
                    result.put(tableName, rowCount);
                }
            }
        } catch (Exception e) {
            log.warn("Could not read table row counts: {}", e.getMessage());
        }
        return result;
    }

    // ── Private Helpers — Redis

    /**
     * Parse a long value from Redis INFO Properties object.
     * Returns 0 if the key is missing or unparseable.
     */
    private long parseLong(Properties info, String key) {
        try {
            String value = info.getProperty(key);
            if (value == null || value.isBlank()) return 0L;
            // Some values include units like "1024kb" — strip non-numeric suffix
            String numeric = value.replaceAll("[^0-9]", "");
            return numeric.isEmpty() ? 0L : Long.parseLong(numeric);
        } catch (NumberFormatException e) {
            log.debug("Could not parse Redis INFO field {}: {}", key, info.getProperty(key));
            return 0L;
        }
    }

    /**
     * Count Redis keys matching a pattern.
     * KEYS is O(N) — acceptable for admin analytics. Not for hot paths.
     */
    private long countRedisKeys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys == null ? 0L : keys.size();
        } catch (Exception e) {
            log.debug("Could not count Redis keys for pattern {}: {}", pattern, e.getMessage());
            return 0L;
        }
    }

    /** Format bytes to human-readable string: 1024 → "1 KB", 1048576 → "1 MB" */
    private String formatBytes(long bytes) {
        if (bytes <= 0)               return "0 B";
        if (bytes < 1024)             return bytes + " B";
        if (bytes < 1024 * 1024)      return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /** Safe empty DTO returned when Redis is unreachable. */
    private RedisMetricsDTO emptyRedisMetrics() {
        return new RedisMetricsDTO(
                0L, 0L, "0 B", 0.0,
                0L, 0L, 0.0,
                0L, 0L, 0L, 0L,
                0,
                "unknown", 0L
        );
    }

}
