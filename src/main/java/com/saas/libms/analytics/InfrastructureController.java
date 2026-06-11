package com.saas.libms.analytics;

import com.saas.libms.analytics.dto.DatabaseMetricsDTO;
import com.saas.libms.analytics.dto.JvmMetricsDTO;
import com.saas.libms.analytics.dto.RedisMetricsDTO;
import com.saas.libms.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/analytics/infrastructure")
@RequiredArgsConstructor
public class InfrastructureController {

    private final InfrastructureService infrastructureService;

    /**
     * JVM metrics: heap usage, non-heap, threads, CPU, uptime.
     *
     * Typical use: the "JVM Health" card on the infrastructure page.
     * Heap usage > 85% and CPU > 80% are warning thresholds.
     */
    @GetMapping("/jvm")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<JvmMetricsDTO>> getJvmMetrics() {
        JvmMetricsDTO data = infrastructureService.getJvmMetrics();
        return ResponseEntity.ok(ApiResponse.success("JVM metrics retrieved", data));
    }

    /**
     * Database metrics: DB size, HikariCP connection pool state, table row counts.
     *
     * Typical use: "Database Health" card + table sizes breakdown.
     * pendingConnections > 0 means the pool is under pressure.
     * tableRowCounts is the "Storage Usage" breakdown table.
     */
    @GetMapping("/database")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<DatabaseMetricsDTO>> getDatabaseMetrics() {
        DatabaseMetricsDTO data = infrastructureService.getDatabaseMetrics();
        return ResponseEntity.ok(ApiResponse.success("Database metrics retrieved", data));
    }

    /**
     * Redis metrics: memory, cache hit rate, key counts by category, version, uptime.
     *
     * Typical use: "Redis Health" card on the infrastructure page.
     * hitRatePercent < 50% is a warning — something is evicting too aggressively.
     * cacheKeys and rateLimitKeys help confirm both Phase 20 and Phase 21 are working.
     */
    @GetMapping("/redis")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<RedisMetricsDTO>> getRedisMetrics() {
        RedisMetricsDTO data = infrastructureService.getRedisMetrics();
        return ResponseEntity.ok(ApiResponse.success("Redis metrics retrieved", data));
    }
}
