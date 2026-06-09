package com.saas.libms.analytics;

// ══════════════════════════════════════════════════════════════════════════════
// Phase 22-B Test Classes
// Run: mvn test -Dtest="Phase22BAnalyticsServiceTest,RequestMetricsFilterTest,BookViewEventServiceTest"
// ══════════════════════════════════════════════════════════════════════════════

import com.saas.libms.analytics.dto.*;
import com.saas.libms.audit.AuditLogRepository;
import com.saas.libms.book.BookRepository;
import com.saas.libms.institution.InstitutionRepository;
import com.saas.libms.loan.LoanRepository;
import com.saas.libms.loan.LoanStatus;
import com.saas.libms.member.MemberRepository;
import com.saas.libms.reservation.ReservationRepository;
import com.saas.libms.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ══════════════════════════════════════════════════════════════════════════════
// TEST CLASS 1 — New AnalyticsService methods added in Phase 22-B
// ══════════════════════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
class Phase22BAnalyticsServiceTest {

    /*
    @Mock private InstitutionRepository   institutionRepository;
    @Mock private UserRepository          userRepository;
    @Mock private BookRepository          bookRepository;
    @Mock private MemberRepository        memberRepository;
    @Mock private LoanRepository          loanRepository;
    @Mock private ReservationRepository   reservationRepository;
    @Mock private AuditLogRepository      auditLogRepository;
    @Mock private StringRedisTemplate     redisTemplate;
    @Mock private ApiRequestLogRepository apiRequestLogRepository;
    @Mock private BookViewEventRepository bookViewEventRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    // ── getTopViewedBooks() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getTopViewedBooks()")
    class GetTopViewedBooksTests {

        @Test
        @DisplayName("Should call platform-wide query when institutionId is null")
        void shouldCallPlatformWideQuery() {
            when(bookViewEventRepository.findTopViewedBooks(any(), any(Pageable.class)))
                    .thenReturn(List.of());

            analyticsService.getTopViewedBooks(10, null, 30);

            verify(bookViewEventRepository).findTopViewedBooks(any(), any(Pageable.class));
            verify(bookViewEventRepository, never())
                    .findTopViewedBooksByInstitution(any(), any(), any());
        }

        @Test
        @DisplayName("Should call institution-filtered query when institutionId is provided")
        void shouldCallFilteredQuery() {
            UUID instId = UUID.randomUUID();
            when(bookViewEventRepository.findTopViewedBooksByInstitution(eq(instId), any(), any()))
                    .thenReturn(List.of());

            analyticsService.getTopViewedBooks(10, instId, 30);

            verify(bookViewEventRepository).findTopViewedBooksByInstitution(eq(instId), any(), any());
            verify(bookViewEventRepository, never()).findTopViewedBooks(any(), any());
        }

        @Test
        @DisplayName("Should map rows to TopViewedBookDTO correctly")
        void shouldMapRowsCorrectly() {
            UUID instId = UUID.randomUUID();
            when(bookViewEventRepository.findTopViewedBooks(any(), any(Pageable.class)))
                    .thenReturn(List.of(
                            new Object[]{"BOOK-001", "Clean Code", instId, 99L},
                            new Object[]{"BOOK-002", "Refactoring", instId, 55L}
                    ));

            List<TopViewedBookDTO> result = analyticsService.getTopViewedBooks(10, null, 30);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).publicId()).isEqualTo("BOOK-001");
            assertThat(result.get(0).title()).isEqualTo("Clean Code");
            assertThat(result.get(0).viewCount()).isEqualTo(99L);
            assertThat(result.get(1).viewCount()).isEqualTo(55L);
        }

        @Test
        @DisplayName("Should cap limit at 50")
        void shouldCapLimitAtFifty() {
            when(bookViewEventRepository.findTopViewedBooks(any(), any(Pageable.class)))
                    .thenReturn(List.of());

            analyticsService.getTopViewedBooks(999, null, 30);

            verify(bookViewEventRepository).findTopViewedBooks(
                    any(),
                    argThat(p -> p.getPageSize() == 50));
        }

        @Test
        @DisplayName("Should return empty list when no view events exist")
        void shouldReturnEmptyWhenNoViews() {
            when(bookViewEventRepository.findTopViewedBooks(any(), any(Pageable.class)))
                    .thenReturn(List.of());

            List<TopViewedBookDTO> result = analyticsService.getTopViewedBooks(10, null, 30);

            assertThat(result).isEmpty();
        }
    }

    // ── getBookViewsTrend() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getBookViewsTrend()")
    class GetBookViewsTrendTests {

        @Test
        @DisplayName("Should return days+1 entries")
        void shouldReturnCorrectSize() {
            when(bookViewEventRepository.countViewsPerDay(any())).thenReturn(List.of());

            List<DailyCountDTO> result = analyticsService.getBookViewsTrend(7);

            assertThat(result).hasSize(8);
        }

        @Test
        @DisplayName("Should zero-fill days with no view events")
        void shouldZeroFillMissingDays() {
            when(bookViewEventRepository.countViewsPerDay(any())).thenReturn(List.of());

            List<DailyCountDTO> result = analyticsService.getBookViewsTrend(5);

            assertThat(result).allSatisfy(dto -> assertThat(dto.count()).isZero());
        }

        @Test
        @DisplayName("Should include actual view counts for days with data")
        void shouldIncludeActualCounts() {
            LocalDate today = LocalDate.now();
            when(bookViewEventRepository.countViewsPerDay(any())).thenReturn(List.of(
                    new Object[]{today, 42L}
            ));

            List<DailyCountDTO> result = analyticsService.getBookViewsTrend(7);

            Optional<DailyCountDTO> todayEntry =
                    result.stream().filter(d -> d.date().equals(today)).findFirst();
            assertThat(todayEntry).isPresent();
            assertThat(todayEntry.get().count()).isEqualTo(42L);
        }

        @Test
        @DisplayName("Last entry should always be today")
        void lastEntryShouldBeToday() {
            when(bookViewEventRepository.countViewsPerDay(any())).thenReturn(List.of());

            List<DailyCountDTO> result = analyticsService.getBookViewsTrend(14);

            assertThat(result.get(result.size() - 1).date()).isEqualTo(LocalDate.now());
        }
    }

    // ── getTrafficSummary() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getTrafficSummary()")
    class GetTrafficSummaryTests {

        @Test
        @DisplayName("Should assemble all fields from repository results")
        void shouldAssembleAllFields() {
            when(apiRequestLogRepository.countRequestsSince(any()))
                    .thenReturn(5L)   // last minute
                    .thenReturn(300L) // last hour
                    .thenReturn(800L); // today
            when(apiRequestLogRepository.findAverageResponseTime(any())).thenReturn(145.5);
            when(apiRequestLogRepository.countByStatusCode(any())).thenReturn(List.of(
                    new Object[]{200, 250L},
                    new Object[]{404, 30L},
                    new Object[]{500, 5L}
            ));

            TrafficSummaryDTO result = analyticsService.getTrafficSummary();

            assertThat(result.requestsLastMinute()).isEqualTo(5L);
            assertThat(result.requestsLastHour()).isEqualTo(300L);
            assertThat(result.requestsToday()).isEqualTo(800L);
            assertThat(result.avgResponseTimeMs()).isEqualTo(145.5);
            assertThat(result.errorCountLastHour()).isEqualTo(35L); // 30 + 5
        }

        @Test
        @DisplayName("Error rate should be calculated as (4xx+5xx)/total*100")
        void errorRateShouldBeCalculatedCorrectly() {
            when(apiRequestLogRepository.countRequestsSince(any()))
                    .thenReturn(0L).thenReturn(100L).thenReturn(0L);
            when(apiRequestLogRepository.findAverageResponseTime(any())).thenReturn(100.0);
            when(apiRequestLogRepository.countByStatusCode(any())).thenReturn(List.of(
                    new Object[]{200, 90L},
                    new Object[]{500, 10L}
            ));

            TrafficSummaryDTO result = analyticsService.getTrafficSummary();

            // 10 errors / 100 total * 100 = 10%
            assertThat(result.errorRatePercent()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Should return zero error rate when no requests")
        void shouldReturnZeroErrorRateWhenNoRequests() {
            when(apiRequestLogRepository.countRequestsSince(any())).thenReturn(0L);
            when(apiRequestLogRepository.findAverageResponseTime(any())).thenReturn(null);
            when(apiRequestLogRepository.countByStatusCode(any())).thenReturn(List.of());

            TrafficSummaryDTO result = analyticsService.getTrafficSummary();

            assertThat(result.errorRatePercent()).isZero();
            assertThat(result.avgResponseTimeMs()).isZero();
            assertThat(result.errorCountLastHour()).isZero();
        }

        @Test
        @DisplayName("Should handle null average response time from repository")
        void shouldHandleNullAverageResponseTime() {
            when(apiRequestLogRepository.countRequestsSince(any())).thenReturn(10L);
            when(apiRequestLogRepository.findAverageResponseTime(any())).thenReturn(null);
            when(apiRequestLogRepository.countByStatusCode(any())).thenReturn(List.of());

            TrafficSummaryDTO result = analyticsService.getTrafficSummary();

            assertThat(result.avgResponseTimeMs()).isZero();
        }
    }

    // ── getErrorRates() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getErrorRates()")
    class GetErrorRatesTests {

        @Test
        @DisplayName("Should correctly assign counts to 2xx, 4xx, 5xx buckets")
        void shouldAssignCountsCorrectly() {
            when(apiRequestLogRepository.countByStatusCodeBucket(any())).thenReturn(List.of(
                    new Object[]{"2xx", 800L},
                    new Object[]{"4xx", 150L},
                    new Object[]{"5xx", 50L}
            ));

            ErrorRateDTO result = analyticsService.getErrorRates(7);

            assertThat(result.count2xx()).isEqualTo(800L);
            assertThat(result.count4xx()).isEqualTo(150L);
            assertThat(result.count5xx()).isEqualTo(50L);
            assertThat(result.totalRequests()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("Error rate percent should be (4xx+5xx)/total*100")
        void shouldCalculateErrorRateCorrectly() {
            when(apiRequestLogRepository.countByStatusCodeBucket(any())).thenReturn(List.of(
                    new Object[]{"2xx", 900L},
                    new Object[]{"4xx", 80L},
                    new Object[]{"5xx", 20L}
            ));

            ErrorRateDTO result = analyticsService.getErrorRates(7);

            // (80 + 20) / 1000 * 100 = 10.0
            assertThat(result.errorRatePercent()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Should return zeros and 0% error rate when no data")
        void shouldReturnZerosWhenNoData() {
            when(apiRequestLogRepository.countByStatusCodeBucket(any())).thenReturn(List.of());

            ErrorRateDTO result = analyticsService.getErrorRates(7);

            assertThat(result.count2xx()).isZero();
            assertThat(result.count4xx()).isZero();
            assertThat(result.count5xx()).isZero();
            assertThat(result.totalRequests()).isZero();
            assertThat(result.errorRatePercent()).isZero();
        }

        @Test
        @DisplayName("Should handle partial bucket results — missing buckets default to zero")
        void shouldHandlePartialBuckets() {
            // Only 4xx returned — no 2xx or 5xx rows
            when(apiRequestLogRepository.countByStatusCodeBucket(any())).thenReturn(List.of(
                    new Object[]{"4xx", 50L}
            ));

            ErrorRateDTO result = analyticsService.getErrorRates(7);

            assertThat(result.count2xx()).isZero();
            assertThat(result.count4xx()).isEqualTo(50L);
            assertThat(result.count5xx()).isZero();
        }
    }

    // ── getTopEndpoints() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTopEndpoints()")
    class GetTopEndpointsTests {

        @Test
        @DisplayName("Should map rows to EndpointMetricsDTO correctly")
        void shouldMapRowsCorrectly() {
            when(apiRequestLogRepository.findTopEndpoints(any(), any(Pageable.class)))
                    .thenReturn(List.of(
                            new Object[]{"GET /api/v1/books/{id}", 5000L},
                            new Object[]{"POST /api/v1/auth/login", 1200L}
                    ));

            List<EndpointMetricsDTO> result = analyticsService.getTopEndpoints(10, 7);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).endpoint()).isEqualTo("GET /api/v1/books/{id}");
            assertThat(result.get(0).callCount()).isEqualTo(5000L);
            assertThat(result.get(1).endpoint()).isEqualTo("POST /api/v1/auth/login");
            assertThat(result.get(1).callCount()).isEqualTo(1200L);
        }

        @Test
        @DisplayName("Should cap limit at 50")
        void shouldCapLimit() {
            when(apiRequestLogRepository.findTopEndpoints(any(), any(Pageable.class)))
                    .thenReturn(List.of());

            analyticsService.getTopEndpoints(200, 7);

            verify(apiRequestLogRepository).findTopEndpoints(
                    any(),
                    argThat(p -> p.getPageSize() == 50));
        }

        @Test
        @DisplayName("Should return empty list when no request logs")
        void shouldReturnEmptyWhenNoLogs() {
            when(apiRequestLogRepository.findTopEndpoints(any(), any(Pageable.class)))
                    .thenReturn(List.of());

            List<EndpointMetricsDTO> result = analyticsService.getTopEndpoints(10, 7);

            assertThat(result).isEmpty();
        }
    }

    // ── getSlowestEndpoints() ────────────────────────────────────────────────

    @Nested
    @DisplayName("getSlowestEndpoints()")
    class GetSlowestEndpointsTests {

        @Test
        @DisplayName("Should map avgDurationMs from repository result")
        void shouldMapAvgDurationMs() {
            when(apiRequestLogRepository.findSlowestEndpoints(any(), any(Pageable.class)))
                    .thenReturn(List.of(
                            new Object[]{"GET /api/v1/system/analytics/institutions/activity", 980.5},
                            new Object[]{"GET /api/v1/loans", 540.2}
                    ));

            List<EndpointMetricsDTO> result = analyticsService.getSlowestEndpoints(10, 7);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).avgDurationMs()).isEqualTo(980.5);
            assertThat(result.get(1).avgDurationMs()).isEqualTo(540.2);
        }

        @Test
        @DisplayName("Should cap limit at 50")
        void shouldCapLimit() {
            when(apiRequestLogRepository.findSlowestEndpoints(any(), any(Pageable.class)))
                    .thenReturn(List.of());

            analyticsService.getSlowestEndpoints(100, 7);

            verify(apiRequestLogRepository).findSlowestEndpoints(
                    any(),
                    argThat(p -> p.getPageSize() == 50));
        }
    }

    // ── getTrafficTrend() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTrafficTrend()")
    class GetTrafficTrendTests {

        @Test
        @DisplayName("Should return days+1 entries")
        void shouldReturnCorrectSize() {
            when(apiRequestLogRepository.countRequestsPerDay(any())).thenReturn(List.of());

            List<DailyCountDTO> result = analyticsService.getTrafficTrend(30);

            assertThat(result).hasSize(31);
        }

        @Test
        @DisplayName("Should zero-fill days with no traffic")
        void shouldZeroFillMissingDays() {
            when(apiRequestLogRepository.countRequestsPerDay(any())).thenReturn(List.of());

            List<DailyCountDTO> result = analyticsService.getTrafficTrend(7);

            assertThat(result).allSatisfy(dto -> assertThat(dto.count()).isZero());
        }

        @Test
        @DisplayName("Should include actual request counts for days with data")
        void shouldIncludeActualCounts() {
            LocalDate today = LocalDate.now();
            when(apiRequestLogRepository.countRequestsPerDay(any())).thenReturn(List.of(
                    new Object[]{today, 1500L}
            ));

            List<DailyCountDTO> result = analyticsService.getTrafficTrend(7);

            Optional<DailyCountDTO> todayEntry =
                    result.stream().filter(d -> d.date().equals(today)).findFirst();
            assertThat(todayEntry).isPresent();
            assertThat(todayEntry.get().count()).isEqualTo(1500L);
        }

        @Test
        @DisplayName("Dates should be in ascending order (oldest first)")
        void datesShouldBeAscending() {
            when(apiRequestLogRepository.countRequestsPerDay(any())).thenReturn(List.of());

            List<DailyCountDTO> result = analyticsService.getTrafficTrend(10);

            for (int i = 0; i < result.size() - 1; i++) {
                assertThat(result.get(i).date()).isBefore(result.get(i + 1).date());
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TEST CLASS 2 — RequestMetricsFilter endpoint normalization
// ══════════════════════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
class RequestMetricsFilterTest {

    // We test the normalizeEndpoint logic by making it package-private
    // OR by testing indirectly through the filter.
    // Since normalizeEndpoint is private, we verify behavior by checking
    // what gets passed to the writer service.

    @Mock private RequestMetricsWriterService writerService;
    @InjectMocks private RequestMetricsFilter filter;

    @Test
    @DisplayName("shouldNotFilter should return true for /actuator paths")
    void shouldSkipActuatorPaths() throws Exception {
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/actuator/health");

        boolean skip = filter.shouldNotFilter(request);

        assertThat(skip).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter should return false for API paths")
    void shouldNotSkipApiPaths() throws Exception {
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/books");

        boolean skip = filter.shouldNotFilter(request);

        assertThat(skip).isFalse();
    }

    @Test
    @DisplayName("shouldNotFilter should return true for /error path")
    void shouldSkipErrorPath() throws Exception {
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/error");

        boolean skip = filter.shouldNotFilter(request);

        assertThat(skip).isTrue();
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TEST CLASS 3 — BookViewEventService async write behavior
// ══════════════════════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
class BookViewEventServiceTest {

    @Mock private BookViewEventRepository bookViewEventRepository;
    @InjectMocks private BookViewEventService bookViewEventService;

    private com.saas.libms.book.Book mockBook() {
        UUID bookId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();

        var institution = mock(com.saas.libms.institution.Institution.class);
        when(institution.getId()).thenReturn(instId);

        var book = mock(com.saas.libms.book.Book.class);
        when(book.getId()).thenReturn(bookId);
        when(book.getPublicId()).thenReturn("BOOK-TEST1");
        when(book.getTitle()).thenReturn("Clean Code");
        when(book.getInstitution()).thenReturn(institution);

        return book;
    }

    @Test
    @DisplayName("Should save a BookViewEvent with correct field values")
    void shouldSaveViewEventWithCorrectFields() {
        var book = mockBook();
        when(bookViewEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookViewEventService.record(book);

        verify(bookViewEventRepository).save(argThat(event ->
                event.getBookPublicId().equals("BOOK-TEST1")
                        && event.getBookTitle().equals("Clean Code")
                        && event.getBookId().equals(book.getId())
                        && event.getInstitutionId().equals(book.getInstitution().getId())
        ));
    }

    @Test
    @DisplayName("Should NOT throw when repository save fails — analytics must never crash the caller")
    void shouldNotThrowWhenSaveFails() {
        var book = mockBook();
        when(bookViewEventRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Must not throw
        assertThatCode(() -> bookViewEventService.record(book))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should call repository.save() exactly once per record() call")
    void shouldCallSaveExactlyOnce() {
        var book = mockBook();
        when(bookViewEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bookViewEventService.record(book);

        verify(bookViewEventRepository, times(1)).save(any());
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TEST CLASS 4 — RequestMetricsWriterService async write behavior
// ══════════════════════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
class RequestMetricsWriterServiceTest {

    @Mock private ApiRequestLogRepository apiRequestLogRepository;
    @InjectMocks private RequestMetricsWriterService writerService;

    @Test
    @DisplayName("Should save ApiRequestLog with all provided fields")
    void shouldSaveWithAllFields() {
        UUID instId = UUID.randomUUID();
        when(apiRequestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writerService.write("GET /api/v1/books/{id}", 200, 85L, instId, "192.168.1.1");

        verify(apiRequestLogRepository).save(argThat(log ->
                log.getEndpoint().equals("GET /api/v1/books/{id}")
                        && log.getStatusCode() == 200
                        && log.getDurationMs() == 85L
                        && log.getInstitutionId().equals(instId)
                        && log.getClientIp().equals("192.168.1.1")
        ));
    }

    @Test
    @DisplayName("Should save with null institutionId for unauthenticated requests")
    void shouldAcceptNullInstitutionId() {
        when(apiRequestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writerService.write("POST /api/v1/auth/login", 401, 12L, null, "10.0.0.1");

        verify(apiRequestLogRepository).save(argThat(log ->
                log.getInstitutionId() == null
                        && log.getStatusCode() == 401
        ));
    }

    @Test
    @DisplayName("Should NOT throw when repository save fails")
    void shouldNotThrowWhenSaveFails() {
        when(apiRequestLogRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatCode(() ->
                writerService.write("GET /api/v1/books", 200, 50L, null, "1.2.3.4"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should call save exactly once per write() call")
    void shouldCallSaveOnce() {
        when(apiRequestLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writerService.write("GET /api/v1/authors", 200, 30L, UUID.randomUUID(), "5.5.5.5");

        verify(apiRequestLogRepository, times(1)).save(any());
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TEST CLASS 5 — AnalyticsCleanupScheduler
// ══════════════════════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
class AnalyticsCleanupSchedulerTest {

    @Mock private ApiRequestLogRepository  apiRequestLogRepository;
    @Mock private BookViewEventRepository  bookViewEventRepository;
    @InjectMocks private AnalyticsCleanupScheduler scheduler;

    @Test
    @DisplayName("Should delete request logs and view events older than 90 days")
    void shouldDeleteBothTablesOlderThan90Days() {
        when(apiRequestLogRepository.deleteOlderThan(any())).thenReturn(500);
        when(bookViewEventRepository.deleteOlderThan(any())).thenReturn(200);

        scheduler.cleanOldAnalyticsData();

        verify(apiRequestLogRepository).deleteOlderThan(
                argThat(cutoff -> cutoff.isBefore(LocalDateTime.now().minusDays(89))));
        verify(bookViewEventRepository).deleteOlderThan(
                argThat(cutoff -> cutoff.isBefore(LocalDateTime.now().minusDays(89))));
    }

    @Test
    @DisplayName("Should call both repositories even if one returns zero deleted rows")
    void shouldCallBothRepositoriesAlways() {
        when(apiRequestLogRepository.deleteOlderThan(any())).thenReturn(0);
        when(bookViewEventRepository.deleteOlderThan(any())).thenReturn(0);

        scheduler.cleanOldAnalyticsData();

        verify(apiRequestLogRepository).deleteOlderThan(any());
        verify(bookViewEventRepository).deleteOlderThan(any());
    }

    @Test
    @DisplayName("Should NOT throw when repositories return zero rows deleted")
    void shouldHandleZeroDeletedGracefully() {
        when(apiRequestLogRepository.deleteOlderThan(any())).thenReturn(0);
        when(bookViewEventRepository.deleteOlderThan(any())).thenReturn(0);

        assertThatCode(() -> scheduler.cleanOldAnalyticsData())
                .doesNotThrowAnyException();
    }*/
}