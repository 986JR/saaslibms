package com.saas.libms.analytics;

import com.saas.libms.analytics.dto.*;
import com.saas.libms.audit.AuditLogRepository;
import com.saas.libms.book.BookRepository;
import com.saas.libms.institution.Institution;
import com.saas.libms.institution.InstitutionRepository;
import com.saas.libms.loan.LoanRepository;
import com.saas.libms.loan.LoanStatus;
import com.saas.libms.member.MemberRepository;
import com.saas.libms.reservation.ReservationRepository;
import com.saas.libms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.libms.analytics.dto.EndpointMetricsDTO;
//   import com.saas.libms.analytics.dto.ErrorRateDTO;
//   import com.saas.libms.analytics.dto.TopViewedBookDTO;
//   import com.saas.libms.analytics.dto.TrafficSummaryDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {
    private final InstitutionRepository  institutionRepository;
    private final UserRepository         userRepository;
    private final BookRepository         bookRepository;
    private final MemberRepository       memberRepository;
    private final LoanRepository         loanRepository;
    private final ReservationRepository  reservationRepository;
    private final AuditLogRepository     auditLogRepository;
    private final StringRedisTemplate    redisTemplate;

    private final ApiRequestLogRepository apiRequestLogRepository;
    private final BookViewEventRepository bookViewEventRepository;


    // 1. Platform Summary

    /**
     * Top-row KPI cards.
     * 12 COUNT queries — all hit indexed columns, all fast.
     */
    public PlatformSummaryDTO getPlatformSummary() {
        return new PlatformSummaryDTO(
                institutionRepository.count(),
                institutionRepository.countByStatusActive(),
                institutionRepository.countByStatusSuspended(),
                userRepository.count(),
                userRepository.countActiveInstitutionUsers(),
                bookRepository.countAllBooks(),
                memberRepository.count(),
                loanRepository.count(),
                loanRepository.countActiveLoans(),
                loanRepository.countOverdueLoans(),
                reservationRepository.count(),
                reservationRepository.countPendingReservations()
        );
    }

    //2. Institution Activity Ranking
    public List<InstitutionActivityDTO> getInstitutionActivity() {
        // Load all institutions — we need name, status, createdAt
        List<Institution> institutions = institutionRepository.findAll();
        if (institutions.isEmpty()) return List.of();

        // Build lookup maps: institutionId → count
        Map<UUID, Long> userCounts        = toMap(userRepository.countUsersPerInstitution());
        Map<UUID, Long> bookCounts        = toMap(bookRepository.countBooksPerInstitution());
        Map<UUID, Long> memberCounts      = toMap(memberRepository.countMembersPerInstitution());
        Map<UUID, Long> loanCounts        = toMap(loanRepository.countLoansPerInstitution());
        Map<UUID, Long> reservationCounts = toMap(reservationRepository.countReservationsPerInstitution());
        Map<UUID, Long> auditCounts       = toMap(auditLogRepository.countActionsPerInstitution());

        return institutions.stream()
                .map(inst -> new InstitutionActivityDTO(
                        inst.getPublicId(),
                        inst.getName(),
                        inst.getStatus().name(),
                        userCounts.getOrDefault(inst.getId(), 0L),
                        bookCounts.getOrDefault(inst.getId(), 0L),
                        memberCounts.getOrDefault(inst.getId(), 0L),
                        loanCounts.getOrDefault(inst.getId(), 0L),
                        reservationCounts.getOrDefault(inst.getId(), 0L),
                        auditCounts.getOrDefault(inst.getId(), 0L),
                        inst.getCreatedAt()
                ))
                // Sort by loan count desc — most active institution first
                .sorted(Comparator.comparingLong(InstitutionActivityDTO::loanCount).reversed())
                .collect(Collectors.toList());
    }

    //3. Institution Growth
    /**
     * Institutions registered per day for the last N days.
     * Fills in zero-count days so the line chart has no gaps.
     */
    public List<InstitutionGrowthDTO> getInstitutionGrowth(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        List<Object[]> rows = institutionRepository.countRegistrationsPerDay(from);

        // Build a map from the DB results
        Map<LocalDate, Long> dbMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            dbMap.put((LocalDate) row[0], (Long) row[1]);
        }

        // Fill all days in range — include zero-count days so frontend has
        // a complete date sequence with no gaps
        List<InstitutionGrowthDTO> result = new ArrayList<>();
        for (int i = days; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            result.add(new InstitutionGrowthDTO(date, dbMap.getOrDefault(date, 0L)));
        }
        return result;
    }

    // 4. Top Borrowed Books
    /**
     * Most borrowed books across the platform, or filtered by one institution.
     * @param limit          how many results to return (max 50)
     * @param institutionId  optional filter — null means platform-wide
     */
    public List<TopBookDTO> getTopBorrowedBooks(int limit, UUID institutionId) {
        int safeLimit = Math.min(limit, 50);
        List<Object[]> rows = (institutionId == null)
                ? loanRepository.findTopBorrowedBooks(PageRequest.of(0, safeLimit))
                : loanRepository.findTopBorrowedBooksByInstitution(institutionId, PageRequest.of(0, safeLimit));

        return rows.stream()
                .map(r -> new TopBookDTO(
                        (String) r[0],   // publicId
                        (String) r[1],   // title
                        (String) r[2],   // institutionName
                        (Long)   r[3]    // count
                ))
                .collect(Collectors.toList());
    }

    //5. Top Reserved Books

    public List<TopBookDTO> getTopReservedBooks(int limit, UUID institutionId) {
        int safeLimit = Math.min(limit, 50);
        List<Object[]> rows = (institutionId == null)
                ? reservationRepository.findTopReservedBooks(PageRequest.of(0, safeLimit))
                : reservationRepository.findTopReservedBooksByInstitution(institutionId, PageRequest.of(0, safeLimit));

        return rows.stream()
                .map(r -> new TopBookDTO(
                        (String) r[0],
                        (String) r[1],
                        (String) r[2],
                        (Long)   r[3]
                ))
                .collect(Collectors.toList());
    }

    //6. Least Borrowed Books

    /**
     * Books with the fewest loans — candidates for cleanup or archiving.
     * Only returns books that have had at least one loan (count >= 1).
     * Books with zero loans are not in the loans table at all, so they
     * cannot appear in a GROUP BY on loans. For zero-loan books, use
     * a future Phase 22-B endpoint that queries books LEFT JOIN loans.
     */
    public List<TopBookDTO> getLeastBorrowedBooks(int limit) {
        int safeLimit = Math.min(limit, 50);
        List<Object[]> rows = loanRepository.findLeastBorrowedBooks(PageRequest.of(0, safeLimit));

        return rows.stream()
                .map(r -> new TopBookDTO(
                        (String) r[0],
                        (String) r[1],
                        (String) r[2],
                        (Long)   r[3]
                ))
                .collect(Collectors.toList());
    }

    //7. Loans Trend

    /**
     * Loans created per day for the last N days.
     * Zero-fill applied so the chart has no date gaps.
     */
    public List<DailyCountDTO> getLoansTrend(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        List<Object[]> rows = loanRepository.countLoansPerDay(from);
        return fillDailyGaps(rows, days);
    }

    //8. Reservations Trend

    public List<DailyCountDTO> getReservationsTrend(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        List<Object[]> rows = reservationRepository.countReservationsPerDay(from);
        return fillDailyGaps(rows, days);
    }

    //9. Loan Status Distribution

    public LoanStatusDistributionDTO getLoanStatusDistribution() {
        List<Object[]> rows = loanRepository.countByStatus();

        long borrowed = 0, late = 0, returned = 0;
        for (Object[] row : rows) {
            // row[0] is LoanStatus enum, row[1] is count
            LoanStatus status = (LoanStatus) row[0];
            long count = (Long) row[1];
            switch (status) {
                case BORROWED -> borrowed = count;
                case LATE     -> late     = count;
                case RETURNED -> returned = count;
            }
        }
        return new LoanStatusDistributionDTO(borrowed, late, returned);
    }

    //10. Daily Active Users

    /**
     * Distinct users who performed at least one action per day.
     * Derived from audit_logs (actorId) — only authenticated staff actions.
     * Zero-fill applied.
     */
    public List<DailyCountDTO> getDailyActiveUsers(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        List<Object[]> rows = auditLogRepository.countDailyActiveUsers(from);
        return fillDailyGaps(rows, days);
    }

    //11. Top Active Users

    /**
     * Staff members ranked by number of audit log actions.
     * Useful for identifying most productive librarians.
     */
    public List<TopActiveUserDTO> getTopActiveUsers(int limit) {
        int safeLimit = Math.min(limit, 50);
        List<Object[]> rows = auditLogRepository.findTopActiveUsers(PageRequest.of(0, safeLimit));

        // Build institutionId → name lookup to enrich results
        Map<UUID, String> institutionNames = buildInstitutionNameMap();

        return rows.stream()
                .map(r -> {
                    UUID institutionId = (UUID) r[2];
                    String instName = institutionNames.getOrDefault(institutionId, "Unknown");
                    return new TopActiveUserDTO(
                            (String) r[0],   // actorEmail
                            (String) r[1],   // actorRole
                            instName,
                            (Long)   r[3]    // actionCount
                    );
                })
                .collect(Collectors.toList());
    }

    //12. Rate Limit Violations

    /**
     * Reads current rate limit state from Redis.
     * Counts how many client keys have hit each endpoint's limit right now.
     *
     * Note: this shows the CURRENT window snapshot, not historical data.
     * A key being present in Redis means the client is within the TTL window,
     * not necessarily that they were blocked — we check count vs limit.
     */
    public RateLimitViolationsDTO getRateLimitViolations() {
        // Count keys per prefix — each key = one unique client in that window
        long loginKeys         = countRedisKeys("rl:LOGIN:*");
        long registerKeys      = countRedisKeys("rl:REGISTER:*");
        long verifyKeys        = countRedisKeys("rl:VERIFY:*");
        long forgotPasswordKeys= countRedisKeys("rl:FORGOT_PWD:*");
        long totalKeys         = countRedisKeys("rl:*");

        return new RateLimitViolationsDTO(
                loginKeys,
                registerKeys,
                verifyKeys,
                forgotPasswordKeys,
                totalKeys
        );
    }



    //helpers

    private List<DailyCountDTO> fillDailyGaps(List<Object[]> rows, int days) {
        Map<LocalDate, Long> dbMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            dbMap.put((LocalDate) row[0], (Long) row[1]);
        }

        List<DailyCountDTO> result = new ArrayList<>();
        for (int i = days; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            result.add(new DailyCountDTO(date, dbMap.getOrDefault(date, 0L)));
        }
        return result;
    }

    /**
     * Count Redis keys matching a pattern.
     * KEYS is O(N) — acceptable for analytics; do not use in hot paths.
     */
    private long countRedisKeys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys == null ? 0L : keys.size();
        } catch (Exception e) {
            log.warn("Could not count Redis keys for pattern {}: {}", pattern, e.getMessage());
            return 0L;
        }
    }

    /**
     * Build institutionId → institutionName map.
     * Used to enrich audit log results which only store institutionId.
     */
    private Map<UUID, String> buildInstitutionNameMap() {
        return institutionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Institution::getId,
                        Institution::getName,
                        (a, b) -> a  // merge function — keep first on duplicate (won't happen)
                ));
    }


    /**
     * Convert a List<Object[]> where [0]=UUID and [1]=Long
     * into a Map<UUID, Long> for fast lookup.
     *
     * Used to build institution-level count maps.
     */
    private Map<UUID, Long> toMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            UUID id    = (UUID) row[0];
            Long count = (Long) row[1];
            map.put(id, count);
        }
        return map;
    }


    public List<TopViewedBookDTO> getTopViewedBooks(int limit, UUID institutionId, int days) {
        int safeLimit = Math.min(limit, 50);
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();

        List<Object[]> rows = (institutionId == null)
                ? bookViewEventRepository.findTopViewedBooks(from, PageRequest.of(0, safeLimit))
                : bookViewEventRepository.findTopViewedBooksByInstitution(institutionId, from, PageRequest.of(0, safeLimit));

        return rows.stream()
                .map(r -> new TopViewedBookDTO(
                        (String) r[0],   // bookPublicId
                        (String) r[1],   // bookTitle
                        (UUID)   r[2],   // institutionId
                        (Long)   r[3]    // viewCount
                ))
                .collect(Collectors.toList());
    }

    // 14. Book Views Trend

    /**
     * Book view events per day for the last N days.
     * Useful for spotting spikes in resource interest.
     */
    public List<DailyCountDTO> getBookViewsTrend(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        List<Object[]> rows = bookViewEventRepository.countViewsPerDay(from);
        return fillDailyGaps(rows, days);
    }

    // 15. Traffic Summary

    /**
     * Live traffic KPI values — requests/min, avg response time, error rate.
     * All derived from api_request_logs for recent time windows.
     */
    public TrafficSummaryDTO getTrafficSummary() {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        LocalDateTime oneHourAgo  = LocalDateTime.now().minusHours(1);
        LocalDateTime todayStart  = LocalDate.now().atStartOfDay();

        long requestsLastMinute = apiRequestLogRepository.countRequestsSince(oneMinuteAgo);
        long requestsLastHour   = apiRequestLogRepository.countRequestsSince(oneHourAgo);
        long requestsToday      = apiRequestLogRepository.countRequestsSince(todayStart);

        Double avgMs = apiRequestLogRepository.findAverageResponseTime(oneHourAgo);
        double avgResponseTimeMs = (avgMs != null) ? avgMs : 0.0;

        // Error count = 4xx + 5xx in the last hour
        List<Object[]> statusRows = apiRequestLogRepository.countByStatusCode(oneHourAgo);
        long errorCount = statusRows.stream()
                .filter(r -> ((Integer) r[0]) >= 400)
                .mapToLong(r -> (Long) r[1])
                .sum();

        double errorRatePercent = requestsLastHour > 0
                ? (errorCount * 100.0) / requestsLastHour
                : 0.0;

        return new TrafficSummaryDTO(
                requestsLastMinute,
                requestsLastHour,
                requestsToday,
                Math.round(avgResponseTimeMs * 100.0) / 100.0, // 2 decimal places
                errorCount,
                Math.round(errorRatePercent * 100.0) / 100.0
        );
    }

    // 16. Top Endpoints

    /**
     * Most called API endpoints in the last N days.
     * Returns endpoint name + call count + avg response time.
     */
    public List<EndpointMetricsDTO> getTopEndpoints(int limit, int days) {
        int safeLimit = Math.min(limit, 50);
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        List<Object[]> rows = apiRequestLogRepository.findTopEndpoints(from, PageRequest.of(0, safeLimit));

        return rows.stream()
                .map(r -> new EndpointMetricsDTO(
                        (String) r[0],                   // endpoint
                        (Long)   r[1],                   // callCount
                        0.0                              // avgDurationMs — filled in separately if needed
                ))
                .collect(Collectors.toList());
    }

    // 17. Slowest Endpoints

    /**
     * Endpoints with the highest average response time — performance bottlenecks.
     */
    public List<EndpointMetricsDTO> getSlowestEndpoints(int limit, int days) {
        int safeLimit = Math.min(limit, 50);
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        List<Object[]> rows = apiRequestLogRepository.findSlowestEndpoints(from, PageRequest.of(0, safeLimit));

        return rows.stream()
                .map(r -> new EndpointMetricsDTO(
                        (String) r[0],                          // endpoint
                        0L,                                     // callCount — not in this query
                        ((Number) r[1]).doubleValue()           // avgDurationMs
                ))
                .collect(Collectors.toList());
    }

    // 18. Error Rates

    /**
     * HTTP response status distribution for the last N days.
     * Grouped into 2xx / 4xx / 5xx buckets.
     */
    public ErrorRateDTO getErrorRates(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        List<Object[]> rows = apiRequestLogRepository.countByStatusCodeBucket(from);

        long count2xx = 0, count4xx = 0, count5xx = 0;
        for (Object[] row : rows) {
            String bucket = (String) row[0];
            long count = (Long) row[1];
            switch (bucket) {
                case "2xx" -> count2xx = count;
                case "4xx" -> count4xx = count;
                case "5xx" -> count5xx = count;
            }
        }

        long total = count2xx + count4xx + count5xx;
        double errorRate = total > 0
                ? ((count4xx + count5xx) * 100.0) / total
                : 0.0;

        return new ErrorRateDTO(
                count2xx, count4xx, count5xx, total,
                Math.round(errorRate * 100.0) / 100.0
        );
    }

    //19. Traffic Trend

    /**
     * Total API requests per day for the last N days.
     * Zero-fill applied for days with no traffic.
     */
    public List<DailyCountDTO> getTrafficTrend(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        List<Object[]> rows = apiRequestLogRepository.countRequestsPerDay(from);
        return fillDailyGaps(rows, days);
    }
}
