package com.saas.libms.analytics;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, UUID> {
    // Top endpoints

    /**
     * Most called endpoints, returns [endpoint, callCount].
     * Used for the top-endpoints table.
     */
    @Query("""
        SELECT a.endpoint, COUNT(a)
        FROM ApiRequestLog a
        WHERE a.createdAt >= :from
        GROUP BY a.endpoint
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> findTopEndpoints(@Param("from") LocalDateTime from, Pageable pageable);

    // Slowest endpoints

    /**
     * Slowest endpoints by average response time — returns [endpoint, avgDurationMs].
     * Used to identify performance bottlenecks.
     */
    @Query("""
        SELECT a.endpoint, AVG(a.durationMs)
        FROM ApiRequestLog a
        WHERE a.createdAt >= :from
        GROUP BY a.endpoint
        ORDER BY AVG(a.durationMs) DESC
        """)
    List<Object[]> findSlowestEndpoints(@Param("from") LocalDateTime from, Pageable pageable);

    // Error rates

    /**
     * Request count grouped by status code range.
     * Returns [statusCodeBucket (String), count].
     *
     * Buckets: "2xx", "4xx", "5xx"
     * Uses integer division: statusCode / 100 → 2, 4, 5
     */
    @Query("""
        SELECT CONCAT(CAST(a.statusCode / 100 AS string), 'xx'), COUNT(a)
        FROM ApiRequestLog a
        WHERE a.createdAt >= :from
        GROUP BY a.statusCode / 100
        ORDER BY a.statusCode / 100 ASC
        """)
    List<Object[]> countByStatusCodeBucket(@Param("from") LocalDateTime from);

    /**
     * Raw count of requests per status code — for more granular error analysis.
     * Returns [statusCode, count].
     */
    @Query("""
        SELECT a.statusCode, COUNT(a)
        FROM ApiRequestLog a
        WHERE a.createdAt >= :from
        GROUP BY a.statusCode
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> countByStatusCode(@Param("from") LocalDateTime from);

    // Requests per time period

    /**
     * Total requests per day — for the traffic trend line chart.
     * Returns [LocalDate, count].
     */
    @Query("""
        SELECT CAST(a.createdAt AS LocalDate), COUNT(a)
        FROM ApiRequestLog a
        WHERE a.createdAt >= :from
        GROUP BY CAST(a.createdAt AS LocalDate)
        ORDER BY CAST(a.createdAt AS LocalDate) ASC
        """)
    List<Object[]> countRequestsPerDay(@Param("from") LocalDateTime from);

    /**
     * Total requests in the last N minutes — live requests/minute approximation.
     * Used for the real-time traffic KPI card.
     */
    @Query("""
        SELECT COUNT(a)
        FROM ApiRequestLog a
        WHERE a.createdAt >= :from
        """)
    long countRequestsSince(@Param("from") LocalDateTime from);

    // Average response time

    /**
     * Platform-wide average API response time in ms over the last N minutes.
     * Used for the "Average API Response Time" KPI card.
     */
    @Query("""
        SELECT AVG(a.durationMs)
        FROM ApiRequestLog a
        WHERE a.createdAt >= :from
        """)
    Double findAverageResponseTime(@Param("from") LocalDateTime from);

    // Cleanup

    /**
     * Delete logs older than the given cutoff.
     * Used by ApiRequestLogCleanupScheduler to prevent unbounded table growth.
     */
    @Query("DELETE FROM ApiRequestLog a WHERE a.createdAt < :cutoff")
    @org.springframework.data.jpa.repository.Modifying
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
