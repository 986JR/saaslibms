package com.saas.libms.analytics;

import com.saas.libms.analytics.dto.*;
import com.saas.libms.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.saas.libms.analytics.dto.EndpointMetricsDTO;
//   import com.saas.libms.analytics.dto.ErrorRateDTO;
//   import com.saas.libms.analytics.dto.TopViewedBookDTO;
//   import com.saas.libms.analytics.dto.TrafficSummaryDTO;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/system/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ── Summary ──────────────────────────────────────────────────────────────

    /**
     * Top-row KPI cards.
     * Returns platform-wide counts: institutions, users, books,
     * members, loans (active + overdue), reservations (pending).
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<PlatformSummaryDTO>> getSummary() {
        PlatformSummaryDTO data = analyticsService.getPlatformSummary();
        return ResponseEntity.ok(ApiResponse.success("Platform summary retrieved", data));
    }

    // ── Institutions ─────────────────────────────────────────────────────────

    /**
     * Institution activity ranking table.
     * Returns all institutions sorted by loan count (most active first).
     * Each entry includes user, book, member, loan, reservation, and audit counts.
     */
    @GetMapping("/institutions/activity")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<InstitutionActivityDTO>>> getInstitutionActivity() {
        List<InstitutionActivityDTO> data = analyticsService.getInstitutionActivity();
        return ResponseEntity.ok(ApiResponse.success("Institution activity retrieved", data));
    }

    /**
     * Institution growth line chart.
     * Returns daily registration counts for the last N days (default 30).
     * Zero-count days are included so the chart has no gaps.
     *
     * @param days number of days to look back (1–365, default 30)
     */
    @GetMapping("/institutions/growth")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<InstitutionGrowthDTO>>> getInstitutionGrowth(
            @RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<InstitutionGrowthDTO> data = analyticsService.getInstitutionGrowth(safeDays);
        return ResponseEntity.ok(ApiResponse.success("Institution growth retrieved", data));
    }

    // ── Books / Resources ────────────────────────────────────────────────────

    /**
     * Top borrowed books bar chart.
     * @param limit          number of results (1–50, default 10)
     * @param institutionId  optional UUID to filter by one institution
     */
    @GetMapping("/books/top-borrowed")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<TopBookDTO>>> getTopBorrowedBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) UUID institutionId) {
        List<TopBookDTO> data = analyticsService.getTopBorrowedBooks(limit, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Top borrowed books retrieved", data));
    }

    /**
     * Top reserved books bar chart.
     * @param limit          number of results (1–50, default 10)
     * @param institutionId  optional UUID to filter by one institution
     */
    @GetMapping("/books/top-reserved")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<TopBookDTO>>> getTopReservedBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) UUID institutionId) {
        List<TopBookDTO> data = analyticsService.getTopReservedBooks(limit, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Top reserved books retrieved", data));
    }

    /**
     * Least borrowed books — cleanup / archive candidates.
     * Only includes books that have had at least one loan.
     * @param limit number of results (1–50, default 10)
     */
    @GetMapping("/books/least-borrowed")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<TopBookDTO>>> getLeastBorrowedBooks(
            @RequestParam(defaultValue = "10") int limit) {
        List<TopBookDTO> data = analyticsService.getLeastBorrowedBooks(limit);
        return ResponseEntity.ok(ApiResponse.success("Least borrowed books retrieved", data));
    }

    // ── Loans ────────────────────────────────────────────────────────────────

    /**
     * Loans created per day line chart.
     * @param days number of days to look back (1–365, default 30)
     */
    @GetMapping("/loans/trend")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<DailyCountDTO>>> getLoansTrend(
            @RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<DailyCountDTO> data = analyticsService.getLoansTrend(safeDays);
        return ResponseEntity.ok(ApiResponse.success("Loans trend retrieved", data));
    }

    /**
     * Loan status distribution — BORROWED / LATE / RETURNED counts.
     * Used for the pie/donut chart.
     */
    @GetMapping("/loans/status-distribution")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<LoanStatusDistributionDTO>> getLoanStatusDistribution() {
        LoanStatusDistributionDTO data = analyticsService.getLoanStatusDistribution();
        return ResponseEntity.ok(ApiResponse.success("Loan status distribution retrieved", data));
    }

    // ── Reservations ─────────────────────────────────────────────────────────

    /**
     * Reservations created per day line chart.
     * @param days number of days to look back (1–365, default 30)
     */
    @GetMapping("/reservations/trend")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<DailyCountDTO>>> getReservationsTrend(
            @RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<DailyCountDTO> data = analyticsService.getReservationsTrend(safeDays);
        return ResponseEntity.ok(ApiResponse.success("Reservations trend retrieved", data));
    }

    // ── Users ────────────────────────────────────────────────────────────────

    /**
     * Daily active users line chart.
     * Counts distinct staff members (by actorId) who performed at least one
     * audited action per day. Proxy for actual login/usage activity.
     *
     * @param days number of days to look back (1–365, default 30)
     */
    @GetMapping("/users/active-daily")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<DailyCountDTO>>> getDailyActiveUsers(
            @RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<DailyCountDTO> data = analyticsService.getDailyActiveUsers(safeDays);
        return ResponseEntity.ok(ApiResponse.success("Daily active users retrieved", data));
    }

    /**
     * Top active users table.
     * Ranks staff members by total number of audit log actions.
     * @param limit number of results (1–50, default 10)
     */
    @GetMapping("/users/top-active")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<TopActiveUserDTO>>> getTopActiveUsers(
            @RequestParam(defaultValue = "10") int limit) {
        List<TopActiveUserDTO> data = analyticsService.getTopActiveUsers(limit);
        return ResponseEntity.ok(ApiResponse.success("Top active users retrieved", data));
    }

    // ── Traffic / Rate Limiting ──────────────────────────────────────────────

    /**
     * Rate limit violation snapshot.
     * Shows how many unique client keys are currently in each endpoint's
     * rate limit window in Redis. Useful for spotting active attack attempts.
     */
    @GetMapping("/traffic/rate-limit-violations")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<RateLimitViolationsDTO>> getRateLimitViolations() {
        RateLimitViolationsDTO data = analyticsService.getRateLimitViolations();
        return ResponseEntity.ok(ApiResponse.success("Rate limit violations retrieved", data));
    }


    @GetMapping("/books/top-viewed")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<TopViewedBookDTO>>> getTopViewedBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<TopViewedBookDTO> data = analyticsService.getTopViewedBooks(limit, institutionId, safeDays);
        return ResponseEntity.ok(ApiResponse.success("Top viewed books retrieved", data));
    }

    /**
     * Book views per day — how many book detail fetches happened each day.
     * @param days number of days to look back (default 30)
     */
    @GetMapping("/books/views-trend")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<DailyCountDTO>>> getBookViewsTrend(
            @RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<DailyCountDTO> data = analyticsService.getBookViewsTrend(safeDays);
        return ResponseEntity.ok(ApiResponse.success("Book views trend retrieved", data));
    }

    // ── Traffic — Phase 22-B additions ───────────────────────────────────────

    /**
     * Live traffic KPI cards: requests/min, requests/hour, avg response time, error rate.
     * Always reflects the last minute and last hour — no params needed.
     */
    @GetMapping("/traffic/summary")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<TrafficSummaryDTO>> getTrafficSummary() {
        TrafficSummaryDTO data = analyticsService.getTrafficSummary();
        return ResponseEntity.ok(ApiResponse.success("Traffic summary retrieved", data));
    }

    /**
     * Most called endpoints.
     * @param limit number of results (1–50, default 10)
     * @param days  how far back to look (1–365, default 7)
     */
    @GetMapping("/traffic/top-endpoints")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<EndpointMetricsDTO>>> getTopEndpoints(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "7") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<EndpointMetricsDTO> data = analyticsService.getTopEndpoints(limit, safeDays);
        return ResponseEntity.ok(ApiResponse.success("Top endpoints retrieved", data));
    }

    /**
     * Slowest endpoints by average response time — performance bottlenecks.
     * @param limit number of results (1–50, default 10)
     * @param days  how far back to look (1–365, default 7)
     */
    @GetMapping("/traffic/slowest-endpoints")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<EndpointMetricsDTO>>> getSlowestEndpoints(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "7") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<EndpointMetricsDTO> data = analyticsService.getSlowestEndpoints(limit, safeDays);
        return ResponseEntity.ok(ApiResponse.success("Slowest endpoints retrieved", data));
    }

    /**
     * HTTP response status distribution — 2xx / 4xx / 5xx breakdown.
     * @param days how far back to look (1–365, default 7)
     */
    @GetMapping("/traffic/error-rates")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<ErrorRateDTO>> getErrorRates(
            @RequestParam(defaultValue = "7") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        ErrorRateDTO data = analyticsService.getErrorRates(safeDays);
        return ResponseEntity.ok(ApiResponse.success("Error rates retrieved", data));
    }

    /**
     * Total API requests per day — traffic volume trend line chart.
     * @param days how far back to look (1–365, default 30)
     */
    @GetMapping("/traffic/trend")
    @PreAuthorize("hasRole('SYSTEM')")
    public ResponseEntity<ApiResponse<List<DailyCountDTO>>> getTrafficTrend(
            @RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        List<DailyCountDTO> data = analyticsService.getTrafficTrend(safeDays);
        return ResponseEntity.ok(ApiResponse.success("Traffic trend retrieved", data));
    }
}
