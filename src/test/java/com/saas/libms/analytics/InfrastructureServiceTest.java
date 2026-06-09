package com.saas.libms.analytics;

import com.saas.libms.analytics.dto.DatabaseMetricsDTO;
import com.saas.libms.analytics.dto.JvmMetricsDTO;
import com.saas.libms.analytics.dto.RedisMetricsDTO;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InfrastructureService — Phase 22-C.
 *
 * Strategy per data source:
 *   JVM metrics   → Use a real SimpleMeterRegistry and register test gauges.
 *                   This is cleaner than mocking Micrometer's fluent API.
 *   DB metrics    → Mock EntityManager and native Query results.
 *   Redis metrics → Mock StringRedisTemplate.execute() to return test Properties.
 *
 * Run: mvn test -Dtest=InfrastructureServiceTest
 */
@ExtendWith(MockitoExtension.class)
class InfrastructureServiceTest {

    // Real registry — lets us register test gauges without fighting Micrometer mocks
    private MeterRegistry meterRegistry;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private EntityManager       entityManager;

    private InfrastructureService infrastructureService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        infrastructureService = new InfrastructureService(
                meterRegistry, redisTemplate, entityManager);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // JVM Metrics Tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getJvmMetrics()")
    class GetJvmMetricsTests {

        /** Register a Micrometer gauge in the test registry with one tag. */
        private void registerGauge(String name, String tagKey, String tagValue, double value) {
            Gauge.builder(name, () -> value)
                    .tag(tagKey, tagValue)
                    .register(meterRegistry);
        }

        private void registerGauge(String name, double value) {
            Gauge.builder(name, () -> value)
                    .register(meterRegistry);
        }

        @Test
        @DisplayName("Should correctly read heap used and max values")
        void shouldReadHeapValues() {
            registerGauge("jvm.memory.used", "area", "heap",    512_000_000.0);
            registerGauge("jvm.memory.max",  "area", "heap",  1_000_000_000.0);

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.heapUsedBytes()).isEqualTo(512_000_000L);
            assertThat(result.heapMaxBytes()).isEqualTo(1_000_000_000L);
        }

        @Test
        @DisplayName("Heap used percent should be heapUsed/heapMax*100")
        void shouldCalculateHeapPercent() {
            registerGauge("jvm.memory.used", "area", "heap",   500_000_000.0);
            registerGauge("jvm.memory.max",  "area", "heap", 1_000_000_000.0);

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.heapUsedPercent()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Heap percent should be 0 when heapMax is 0 — avoids division by zero")
        void shouldReturnZeroPercentWhenHeapMaxIsZero() {
            registerGauge("jvm.memory.used", "area", "heap", 100_000.0);
            registerGauge("jvm.memory.max",  "area", "heap",        0.0);

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.heapUsedPercent()).isZero();
        }

        @Test
        @DisplayName("Should read non-heap memory")
        void shouldReadNonHeapMemory() {
            registerGauge("jvm.memory.used", "area", "nonheap", 80_000_000.0);

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.nonHeapUsedBytes()).isEqualTo(80_000_000L);
        }

        @Test
        @DisplayName("Should read thread counts")
        void shouldReadThreadCounts() {
            registerGauge("jvm.threads.live",   25.0);
            registerGauge("jvm.threads.daemon", 18.0);
            registerGauge("jvm.threads.peak",   30.0);

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.liveThreads()).isEqualTo(25);
            assertThat(result.daemonThreads()).isEqualTo(18);
            assertThat(result.peakThreads()).isEqualTo(30);
        }

        @Test
        @DisplayName("CPU usage should be converted from 0.0-1.0 to 0.0-100.0")
        void shouldConvertCpuUsageToPercent() {
            registerGauge("system.cpu.usage", 0.35);  // 35%

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.cpuUsagePercent()).isEqualTo(35.0);
        }

        @Test
        @DisplayName("Should read available processor count")
        void shouldReadProcessorCount() {
            registerGauge("system.cpu.count", 8.0);

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.availableProcessors()).isEqualTo(8);
        }

        @Test
        @DisplayName("Should read uptime in seconds")
        void shouldReadUptimeSeconds() {
            registerGauge("process.uptime", 3661.0); // 1h 1m 1s

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.uptimeSeconds()).isEqualTo(3661L);
        }

        @Test
        @DisplayName("Uptime formatted should be human-readable")
        void shouldFormatUptimeCorrectly() {
            registerGauge("process.uptime", 90070.0); // 1d 1h 1m

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            // 90070s = 1d 1h 1m
            assertThat(result.uptimeFormatted()).isEqualTo("1d 1h 1m");
        }

        @Test
        @DisplayName("Uptime under 1 hour should show only minutes")
        void shouldFormatUptimeMinutesOnly() {
            registerGauge("process.uptime", 600.0); // 10m

            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.uptimeFormatted()).isEqualTo("10m");
        }

        @Test
        @DisplayName("Should return zeros for all fields when no gauges are registered")
        void shouldReturnZerosWhenNoGauges() {
            // No gauges registered — all reads fall back to 0
            JvmMetricsDTO result = infrastructureService.getJvmMetrics();

            assertThat(result.heapUsedBytes()).isZero();
            assertThat(result.heapMaxBytes()).isZero();
            assertThat(result.heapUsedPercent()).isZero();
            assertThat(result.cpuUsagePercent()).isZero();
            assertThat(result.liveThreads()).isZero();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Database Metrics Tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getDatabaseMetrics()")
    class GetDatabaseMetricsTests {

        @Test
        @DisplayName("Should read database size and format it correctly")
        void shouldReadDatabaseSize() {
            Query sizeQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_database_size")))
                    .thenReturn(sizeQuery);
            when(sizeQuery.getSingleResult()).thenReturn(150_000_000L); // 150 MB

            // Also stub the row counts query
            Query rowCountQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_stat_user_tables")))
                    .thenReturn(rowCountQuery);
            when(rowCountQuery.getResultList()).thenReturn(List.of());

            DatabaseMetricsDTO result = infrastructureService.getDatabaseMetrics();

            assertThat(result.databaseSizeBytes()).isEqualTo(150_000_000L);
            assertThat(result.databaseSizeFormatted()).isEqualTo("143.1 MB");
        }

        @Test
        @DisplayName("Should read table row counts from pg_stat_user_tables")
        void shouldReadTableRowCounts() {
            Query sizeQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_database_size")))
                    .thenReturn(sizeQuery);
            when(sizeQuery.getSingleResult()).thenReturn(0L);

            Query rowCountQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_stat_user_tables")))
                    .thenReturn(rowCountQuery);
            when(rowCountQuery.getResultList()).thenReturn(List.of(
                    new Object[]{"books",   500L},
                    new Object[]{"members", 1200L},
                    new Object[]{"loans",   3000L}
            ));

            DatabaseMetricsDTO result = infrastructureService.getDatabaseMetrics();

            assertThat(result.tableRowCounts()).containsEntry("books", 500L);
            assertThat(result.tableRowCounts()).containsEntry("members", 1200L);
            assertThat(result.tableRowCounts()).containsEntry("loans", 3000L);
        }

        @Test
        @DisplayName("Should exclude non-application tables from row count map")
        void shouldExcludeNonAppTables() {
            Query sizeQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_database_size")))
                    .thenReturn(sizeQuery);
            when(sizeQuery.getSingleResult()).thenReturn(0L);

            Query rowCountQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_stat_user_tables")))
                    .thenReturn(rowCountQuery);
            when(rowCountQuery.getResultList()).thenReturn(List.of(
                    new Object[]{"books",           500L},
                    new Object[]{"flyway_schema_history", 12L},  // should be excluded
                    new Object[]{"spring_session",   5L}         // should be excluded
            ));

            DatabaseMetricsDTO result = infrastructureService.getDatabaseMetrics();

            assertThat(result.tableRowCounts()).containsKey("books");
            assertThat(result.tableRowCounts()).doesNotContainKey("flyway_schema_history");
            assertThat(result.tableRowCounts()).doesNotContainKey("spring_session");
        }

        @Test
        @DisplayName("Should return empty map and zero size when DB query fails")
        void shouldHandleDbQueryFailureGracefully() {
            when(entityManager.createNativeQuery(anyString()))
                    .thenThrow(new RuntimeException("DB unavailable"));

            DatabaseMetricsDTO result = infrastructureService.getDatabaseMetrics();

            assertThat(result.databaseSizeBytes()).isZero();
            assertThat(result.tableRowCounts()).isEmpty();
        }

        @Test
        @DisplayName("HikariCP connection values should default to zero when metrics not registered")
        void shouldDefaultHikariToZeroWhenNotRegistered() {
            Query sizeQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_database_size")))
                    .thenReturn(sizeQuery);
            when(sizeQuery.getSingleResult()).thenReturn(0L);

            Query rowCountQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_stat_user_tables")))
                    .thenReturn(rowCountQuery);
            when(rowCountQuery.getResultList()).thenReturn(List.of());

            // No HikariCP gauges registered in test registry
            DatabaseMetricsDTO result = infrastructureService.getDatabaseMetrics();

            assertThat(result.activeConnections()).isZero();
            assertThat(result.idleConnections()).isZero();
            assertThat(result.pendingConnections()).isZero();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Redis Metrics Tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRedisMetrics()")
    class GetRedisMetricsTests {

        /** Build a Redis INFO Properties object with the given key-value pairs. */
        private Properties buildInfoProps(String... pairs) {
            Properties props = new Properties();
            for (int i = 0; i < pairs.length; i += 2) {
                props.setProperty(pairs[i], pairs[i + 1]);
            }
            return props;
        }

        /** Stub redisTemplate.execute() to return the given Properties. */
        @SuppressWarnings("unchecked")
        private void givenRedisInfo(Properties info) {
            when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(info);
            when(redisTemplate.keys(anyString())).thenReturn(Set.of());
        }

        @Test
        @DisplayName("Should read used memory and format it correctly")
        void shouldReadUsedMemory() {
            givenRedisInfo(buildInfoProps(
                    "used_memory",       "25165824",  // 24 MB exactly
                    "maxmemory",         "0",
                    "keyspace_hits",     "0",
                    "keyspace_misses",   "0",
                    "evicted_keys",      "0",
                    "connected_clients", "1",
                    "redis_version",     "8.0.0",
                    "uptime_in_seconds", "3600"
            ));

            RedisMetricsDTO result = infrastructureService.getRedisMetrics();

            assertThat(result.usedMemoryBytes()).isEqualTo(25_165_824L);
            assertThat(result.usedMemoryFormatted()).isEqualTo("24.0 MB");
        }

        @Test
        @DisplayName("Should calculate hit rate correctly from hits and misses")
        void shouldCalculateHitRate() {
            givenRedisInfo(buildInfoProps(
                    "used_memory",       "1024",
                    "maxmemory",         "0",
                    "keyspace_hits",     "75",
                    "keyspace_misses",   "25",  // 75/(75+25) = 75%
                    "evicted_keys",      "0",
                    "connected_clients", "1",
                    "redis_version",     "8.0.0",
                    "uptime_in_seconds", "100"
            ));

            RedisMetricsDTO result = infrastructureService.getRedisMetrics();

            assertThat(result.keyspaceHits()).isEqualTo(75L);
            assertThat(result.keyspaceMisses()).isEqualTo(25L);
            assertThat(result.hitRatePercent()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("Hit rate should be 0 when no lookups have occurred")
        void shouldReturnZeroHitRateWhenNoLookups() {
            givenRedisInfo(buildInfoProps(
                    "used_memory",       "1024",
                    "maxmemory",         "0",
                    "keyspace_hits",     "0",
                    "keyspace_misses",   "0",
                    "evicted_keys",      "0",
                    "connected_clients", "1",
                    "redis_version",     "8.0.0",
                    "uptime_in_seconds", "0"
            ));

            RedisMetricsDTO result = infrastructureService.getRedisMetrics();

            assertThat(result.hitRatePercent()).isZero();
        }

        @Test
        @DisplayName("Memory usage percent should be 0 when maxmemory is 0 (no limit)")
        void shouldReturnZeroMemoryPercentWhenNoLimit() {
            givenRedisInfo(buildInfoProps(
                    "used_memory",       "1048576",
                    "maxmemory",         "0",  // no limit set
                    "keyspace_hits",     "0",
                    "keyspace_misses",   "0",
                    "evicted_keys",      "0",
                    "connected_clients", "1",
                    "redis_version",     "8.0.0",
                    "uptime_in_seconds", "0"
            ));

            RedisMetricsDTO result = infrastructureService.getRedisMetrics();

            assertThat(result.memoryUsagePercent()).isZero();
        }

        @Test
        @DisplayName("Should read Redis version and uptime")
        void shouldReadVersionAndUptime() {
            givenRedisInfo(buildInfoProps(
                    "used_memory",       "1024",
                    "maxmemory",         "0",
                    "keyspace_hits",     "0",
                    "keyspace_misses",   "0",
                    "evicted_keys",      "0",
                    "connected_clients", "3",
                    "redis_version",     "8.0.0",
                    "uptime_in_seconds", "7200"
            ));

            RedisMetricsDTO result = infrastructureService.getRedisMetrics();

            assertThat(result.redisVersion()).isEqualTo("8.0.0");
            assertThat(result.uptimeSeconds()).isEqualTo(7200L);
            assertThat(result.connectedClients()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return empty metrics when Redis returns null INFO")
        void shouldReturnEmptyMetricsWhenInfoIsNull() {
            when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

            RedisMetricsDTO result = infrastructureService.getRedisMetrics();

            assertThat(result.usedMemoryBytes()).isZero();
            assertThat(result.hitRatePercent()).isZero();
            assertThat(result.redisVersion()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("Should return empty metrics and not throw when Redis throws exception")
        void shouldHandleRedisExceptionGracefully() {
            when(redisTemplate.execute(any(RedisCallback.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            assertThatCode(() -> infrastructureService.getRedisMetrics())
                    .doesNotThrowAnyException();

            RedisMetricsDTO result = infrastructureService.getRedisMetrics();
            assertThat(result.usedMemoryBytes()).isZero();
            assertThat(result.totalKeys()).isZero();
        }

        @Test
        @DisplayName("Key counts should be read from Redis KEYS pattern scan")
        void shouldReadKeyCountsFromRedis() {
            when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(
                    buildInfoProps(
                            "used_memory", "1024", "maxmemory", "0",
                            "keyspace_hits", "0", "keyspace_misses", "0",
                            "evicted_keys", "0", "connected_clients", "1",
                            "redis_version", "8.0.0", "uptime_in_seconds", "0"
                    )
            );
            // Stub key pattern scans
            when(redisTemplate.keys("*")).thenReturn(
                    Set.of("books::id1", "rl:LOGIN:ip1", "rl:GLOBAL:ip1"));
            when(redisTemplate.keys("books::*")).thenReturn(Set.of("books::id1"));
            when(redisTemplate.keys("authors::*")).thenReturn(Set.of());
            when(redisTemplate.keys("categories::*")).thenReturn(Set.of());
            when(redisTemplate.keys("members::*")).thenReturn(Set.of());
            when(redisTemplate.keys("rl:*")).thenReturn(
                    Set.of("rl:LOGIN:ip1", "rl:GLOBAL:ip1"));

            RedisMetricsDTO result = infrastructureService.getRedisMetrics();

            assertThat(result.totalKeys()).isEqualTo(3L);
            assertThat(result.cacheKeys()).isEqualTo(1L);
            assertThat(result.rateLimitKeys()).isEqualTo(2L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // formatBytes helper tests (tested via getDatabaseMetrics)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("formatBytes() helper")
    class FormatBytesTests {

        private String formatViaDb(long bytes) {
            Query sizeQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_database_size")))
                    .thenReturn(sizeQuery);
            when(sizeQuery.getSingleResult()).thenReturn(bytes);
            Query rowQuery = mock(Query.class);
            when(entityManager.createNativeQuery(contains("pg_stat_user_tables")))
                    .thenReturn(rowQuery);
            when(rowQuery.getResultList()).thenReturn(List.of());
            return infrastructureService.getDatabaseMetrics().databaseSizeFormatted();
        }

        @Test @DisplayName("0 bytes → '0 B'")
        void zeroBytesFormatsCorrectly() {
            assertThat(formatViaDb(0L)).isEqualTo("0 B");
        }

        @Test @DisplayName("500 bytes → '500 B'")
        void smallBytesFormatsCorrectly() {
            assertThat(formatViaDb(500L)).isEqualTo("500 B");
        }

        @Test @DisplayName("1024 bytes → '1.0 KB'")
        void exactKilobyteFormatsCorrectly() {
            assertThat(formatViaDb(1024L)).isEqualTo("1.0 KB");
        }

        @Test @DisplayName("1048576 bytes → '1.0 MB'")
        void exactMegabyteFormatsCorrectly() {
            assertThat(formatViaDb(1_048_576L)).isEqualTo("1.0 MB");
        }

        @Test @DisplayName("1073741824 bytes → '1.00 GB'")
        void exactGigabyteFormatsCorrectly() {
            assertThat(formatViaDb(1_073_741_824L)).isEqualTo("1.00 GB");
        }
    }
}