package com.saas.libms.analytics;
/**
import com.saas.libms.analytics.dto.*;
import com.saas.libms.audit.AuditLogRepository;
import com.saas.libms.book.BookRepository;
import com.saas.libms.institution.Institution;
import com.saas.libms.institution.InstitutionRepository;
import com.saas.libms.institution.InstitutionStatus;
import com.saas.libms.loan.LoanRepository;
import com.saas.libms.loan.LoanStatus;
import com.saas.libms.member.MemberRepository;
import com.saas.libms.reservation.ReservationRepository;
import com.saas.libms.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


 * Unit tests for AnalyticsService — Phase 22-A.
 *
 * All repositories and Redis are mocked — no real DB or Redis needed.
 * Tests verify:
 *   - correct values are assembled from repository results
 *   - zero-fill logic works for daily trend charts
 *   - sorting works correctly for activity rankings
 *   - institution filtering is applied vs. not applied correctly
 *   - safety limits (max 50) are enforced
 *   - empty repository results are handled gracefully
 *   - Redis failure is handled gracefully (returns 0, not exception)
 *
 * Run: mvn test -Dtest=AnalyticsServiceTest
 */
//@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {
/*
    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock private InstitutionRepository institutionRepository;
    @Mock private UserRepository        userRepository;
    @Mock private BookRepository        bookRepository;
    @Mock private MemberRepository      memberRepository;
    @Mock private LoanRepository        loanRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private AuditLogRepository    auditLogRepository;
    @Mock private StringRedisTemplate   redisTemplate;

    @InjectMocks
    private AnalyticsService analyticsService;

    // ── Shared test data ──────────────────────────────────────────────────────

    private UUID instAId;
    private UUID instBId;
    private Institution instA;
    private Institution instB;

    @BeforeEach
    void setUp() {
        instAId = UUID.randomUUID();
        instBId = UUID.randomUUID();

        instA = mock(Institution.class);
        when(instA.getId()).thenReturn(instAId);
        when(instA.getPublicId()).thenReturn("INST-AAAA");
        when(instA.getName()).thenReturn("University of Dodoma");
        when(instA.getStatus()).thenReturn(InstitutionStatus.ACTIVE);
        when(instA.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(60));

        instB = mock(Institution.class);
        when(instB.getId()).thenReturn(instBId);
        when(instB.getPublicId()).thenReturn("INST-BBBB");
        when(instB.getName()).thenReturn("Dar es Salaam Institute");
        when(instB.getStatus()).thenReturn(InstitutionStatus.ACTIVE);
        when(instB.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(30));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. getPlatformSummary()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPlatformSummary()")
    class GetPlatformSummaryTests {

        @Test
        @DisplayName("Should assemble all 12 KPI values from repository counts")
        void shouldAssembleAllKpiValues() {
            when(institutionRepository.count()).thenReturn(5L);
            when(institutionRepository.countByStatusActive()).thenReturn(4L);
            when(institutionRepository.countByStatusSuspended()).thenReturn(1L);
            when(userRepository.count()).thenReturn(20L);
            when(userRepository.countActiveInstitutionUsers()).thenReturn(18L);
            when(bookRepository.countAllBooks()).thenReturn(300L);
            when(memberRepository.count()).thenReturn(150L);
            when(loanRepository.count()).thenReturn(500L);
            when(loanRepository.countActiveLoans()).thenReturn(45L);
            when(loanRepository.countOverdueLoans()).thenReturn(7L);
            when(reservationRepository.count()).thenReturn(80L);
            when(reservationRepository.countPendingReservations()).thenReturn(12L);

            PlatformSummaryDTO result = analyticsService.getPlatformSummary();

            assertThat(result.totalInstitutions()).isEqualTo(5L);
            assertThat(result.activeInstitutions()).isEqualTo(4L);
            assertThat(result.suspendedInstitutions()).isEqualTo(1L);
            assertThat(result.totalUsers()).isEqualTo(20L);
            assertThat(result.activeUsers()).isEqualTo(18L);
            assertThat(result.totalBooks()).isEqualTo(300L);
            assertThat(result.totalMembers()).isEqualTo(150L);
            assertThat(result.totalLoans()).isEqualTo(500L);
            assertThat(result.activeLoans()).isEqualTo(45L);
            assertThat(result.overdueLoans()).isEqualTo(7L);
            assertThat(result.totalReservations()).isEqualTo(80L);
            assertThat(result.pendingReservations()).isEqualTo(12L);
        }

        @Test
        @DisplayName("Should return zeros when platform is empty (no data yet)")
        void shouldReturnZerosOnEmptyPlatform() {
            when(institutionRepository.count()).thenReturn(0L);
            when(institutionRepository.countByStatusActive()).thenReturn(0L);
            when(institutionRepository.countByStatusSuspended()).thenReturn(0L);
            when(userRepository.count()).thenReturn(0L);
            when(userRepository.countActiveInstitutionUsers()).thenReturn(0L);
            when(bookRepository.countAllBooks()).thenReturn(0L);
            when(memberRepository.count()).thenReturn(0L);
            when(loanRepository.count()).thenReturn(0L);
            when(loanRepository.countActiveLoans()).thenReturn(0L);
            when(loanRepository.countOverdueLoans()).thenReturn(0L);
            when(reservationRepository.count()).thenReturn(0L);
            when(reservationRepository.countPendingReservations()).thenReturn(0L);

            PlatformSummaryDTO result = analyticsService.getPlatformSummary();

            assertThat(result.totalInstitutions()).isZero();
            assertThat(result.totalLoans()).isZero();
            assertThat(result.pendingReservations()).isZero();
        }

        @Test
        @DisplayName("Should call every repository exactly once")
        void shouldCallEveryRepositoryExactlyOnce() {
            when(institutionRepository.count()).thenReturn(1L);
            when(institutionRepository.countByStatusActive()).thenReturn(1L);
            when(institutionRepository.countByStatusSuspended()).thenReturn(0L);
            when(userRepository.count()).thenReturn(2L);
            when(userRepository.countActiveInstitutionUsers()).thenReturn(2L);
            when(bookRepository.countAllBooks()).thenReturn(10L);
            when(memberRepository.count()).thenReturn(5L);
            when(loanRepository.count()).thenReturn(3L);
            when(loanRepository.countActiveLoans()).thenReturn(2L);
            when(loanRepository.countOverdueLoans()).thenReturn(1L);
            when(reservationRepository.count()).thenReturn(4L);
            when(reservationRepository.countPendingReservations()).thenReturn(2L);

            analyticsService.getPlatformSummary();

            verify(institutionRepository).count();
            verify(institutionRepository).countByStatusActive();
            verify(institutionRepository).countByStatusSuspended();
            verify(userRepository).count();
            verify(userRepository).countActiveInstitutionUsers();
            verify(bookRepository).countAllBooks();
            verify(memberRepository).count();
            verify(loanRepository).count();
            verify(loanRepository).countActiveLoans();
            verify(loanRepository).countOverdueLoans();
            verify(reservationRepository).count();
            verify(reservationRepository).countPendingReservations();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. getInstitutionActivity()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getInstitutionActivity()")
    class GetInstitutionActivityTests {

        @Test
        @DisplayName("Should return empty list when no institutions exist")
        void shouldReturnEmptyListWhenNoInstitutions() {
            when(institutionRepository.findAll()).thenReturn(List.of());

            List<InstitutionActivityDTO> result = analyticsService.getInstitutionActivity();

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository, bookRepository, memberRepository,
                    loanRepository, reservationRepository, auditLogRepository);
        }

        @Test
        @DisplayName("Should map all counts correctly from repository results")
        void shouldMapAllCountsCorrectly() {
            when(institutionRepository.findAll()).thenReturn(List.of(instA));
            when(userRepository.countUsersPerInstitution())
                    .thenReturn(List.of(new Object[]{instAId, 5L}));
            when(bookRepository.countBooksPerInstitution())
                    .thenReturn(List.of(new Object[]{instAId, 100L}));
            when(memberRepository.countMembersPerInstitution())
                    .thenReturn(List.of(new Object[]{instAId, 50L}));
            when(loanRepository.countLoansPerInstitution())
                    .thenReturn(List.of(new Object[]{instAId, 200L}));
            when(reservationRepository.countReservationsPerInstitution())
                    .thenReturn(List.of(new Object[]{instAId, 30L}));
            when(auditLogRepository.countActionsPerInstitution())
                    .thenReturn(List.of(new Object[]{instAId, 400L}));

            List<InstitutionActivityDTO> result = analyticsService.getInstitutionActivity();

            assertThat(result).hasSize(1);
            InstitutionActivityDTO dto = result.get(0);
            assertThat(dto.publicId()).isEqualTo("INST-AAAA");
            assertThat(dto.name()).isEqualTo("University of Dodoma");
            assertThat(dto.status()).isEqualTo("ACTIVE");
            assertThat(dto.userCount()).isEqualTo(5L);
            assertThat(dto.bookCount()).isEqualTo(100L);
            assertThat(dto.memberCount()).isEqualTo(50L);
            assertThat(dto.loanCount()).isEqualTo(200L);
            assertThat(dto.reservationCount()).isEqualTo(30L);
            assertThat(dto.auditActionCount()).isEqualTo(400L);
        }

        @Test
        @DisplayName("Should default to zero for any count not returned by repository")
        void shouldDefaultToZeroForMissingCounts() {
            when(institutionRepository.findAll()).thenReturn(List.of(instB));
            when(userRepository.countUsersPerInstitution()).thenReturn(Collections.emptyList());
            when(bookRepository.countBooksPerInstitution()).thenReturn(Collections.emptyList());
            when(memberRepository.countMembersPerInstitution()).thenReturn(Collections.emptyList());
            when(loanRepository.countLoansPerInstitution()).thenReturn(Collections.emptyList());
            when(reservationRepository.countReservationsPerInstitution()).thenReturn(Collections.emptyList());
            when(auditLogRepository.countActionsPerInstitution()).thenReturn(Collections.emptyList());

            List<InstitutionActivityDTO> result = analyticsService.getInstitutionActivity();

            assertThat(result).hasSize(1);
            InstitutionActivityDTO dto = result.get(0);
            assertThat(dto.userCount()).isZero();
            assertThat(dto.bookCount()).isZero();
            assertThat(dto.loanCount()).isZero();
            assertThat(dto.reservationCount()).isZero();
            assertThat(dto.auditActionCount()).isZero();
        }

        @Test
        @DisplayName("Should sort institutions by loanCount descending — most active first")
        void shouldSortByLoanCountDescending() {
            when(institutionRepository.findAll()).thenReturn(List.of(instA, instB));
            when(userRepository.countUsersPerInstitution()).thenReturn(Collections.emptyList());
            when(bookRepository.countBooksPerInstitution()).thenReturn(Collections.emptyList());
            when(memberRepository.countMembersPerInstitution()).thenReturn(Collections.emptyList());
            when(loanRepository.countLoansPerInstitution()).thenReturn(List.of(
                    new Object[]{instAId, 200L},
                    new Object[]{instBId, 500L}
            ));
            when(reservationRepository.countReservationsPerInstitution()).thenReturn(Collections.emptyList());
            when(auditLogRepository.countActionsPerInstitution()).thenReturn(Collections.emptyList());

            List<InstitutionActivityDTO> result = analyticsService.getInstitutionActivity();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Dar es Salaam Institute"); // 500 loans
            assertThat(result.get(1).name()).isEqualTo("University of Dodoma");    // 200 loans
        }

        @Test
        @DisplayName("Should handle multiple institutions correctly — each gets its own counts")
        void shouldHandleMultipleInstitutionsCorrectly() {
            when(institutionRepository.findAll()).thenReturn(List.of(instA, instB));
            when(userRepository.countUsersPerInstitution()).thenReturn(List.of(
                    new Object[]{instAId, 10L},
                    new Object[]{instBId, 3L}
            ));
            when(bookRepository.countBooksPerInstitution()).thenReturn(List.of(
                    new Object[]{instAId, 200L},
                    new Object[]{instBId, 50L}
            ));
            when(memberRepository.countMembersPerInstitution()).thenReturn(Collections.emptyList());
            when(loanRepository.countLoansPerInstitution()).thenReturn(List.of(
                    new Object[]{instAId, 80L},
                    new Object[]{instBId, 20L}
            ));
            when(reservationRepository.countReservationsPerInstitution()).thenReturn(Collections.emptyList());
            when(auditLogRepository.countActionsPerInstitution()).thenReturn(Collections.emptyList());

            List<InstitutionActivityDTO> result = analyticsService.getInstitutionActivity();

            assertThat(result).hasSize(2);
            // After sort: instA (80 loans) first, instB (20 loans) second
            InstitutionActivityDTO first = result.get(0);
            assertThat(first.publicId()).isEqualTo("INST-AAAA");
            assertThat(first.userCount()).isEqualTo(10L);
            assertThat(first.bookCount()).isEqualTo(200L);

            InstitutionActivityDTO second = result.get(1);
            assertThat(second.publicId()).isEqualTo("INST-BBBB");
            assertThat(second.userCount()).isEqualTo(3L);
            assertThat(second.bookCount()).isEqualTo(50L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. getInstitutionGrowth()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getInstitutionGrowth()")
    class GetInstitutionGrowthTests {

        @Test
        @DisplayName("Should return days+1 entries (includes today)")
        void shouldReturnCorrectNumberOfEntries() {
            when(institutionRepository.countRegistrationsPerDay(any()))
                    .thenReturn(Collections.emptyList());

            List<InstitutionGrowthDTO> result = analyticsService.getInstitutionGrowth(7);

            assertThat(result).hasSize(8); // 7 days back + today
        }

        @Test
        @DisplayName("Should fill zero-count days when DB returns no data")
        void shouldFillZeroCountDays() {
            when(institutionRepository.countRegistrationsPerDay(any()))
                    .thenReturn(Collections.emptyList());

            List<InstitutionGrowthDTO> result = analyticsService.getInstitutionGrowth(5);

            assertThat(result).allSatisfy(dto -> assertThat(dto.count()).isZero());
        }

        @Test
        @DisplayName("Should fill actual counts for days that have data")
        void shouldFillActualCountsForDaysWithData() {
            LocalDate today     = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            when(institutionRepository.countRegistrationsPerDay(any())).thenReturn(List.of(
                    new Object[]{today,     3L},
                    new Object[]{yesterday, 1L}
            ));

            List<InstitutionGrowthDTO> result = analyticsService.getInstitutionGrowth(7);

            Optional<InstitutionGrowthDTO> todayEntry =
                    result.stream().filter(d -> d.date().equals(today)).findFirst();
            Optional<InstitutionGrowthDTO> yesterdayEntry =
                    result.stream().filter(d -> d.date().equals(yesterday)).findFirst();

            assertThat(todayEntry).isPresent();
            assertThat(todayEntry.get().count()).isEqualTo(3L);
            assertThat(yesterdayEntry).isPresent();
            assertThat(yesterdayEntry.get().count()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should have dates in ascending order (oldest first)")
        void shouldHaveDatesInAscendingOrder() {
            when(institutionRepository.countRegistrationsPerDay(any()))
                    .thenReturn(Collections.emptyList());

            List<InstitutionGrowthDTO> result = analyticsService.getInstitutionGrowth(5);

            for (int i = 0; i < result.size() - 1; i++) {
                assertThat(result.get(i).date()).isBefore(result.get(i + 1).date());
            }
        }

        @Test
        @DisplayName("Last entry should always be today")
        void lastEntryShouldBeToday() {
            when(institutionRepository.countRegistrationsPerDay(any()))
                    .thenReturn(Collections.emptyList());

            List<InstitutionGrowthDTO> result = analyticsService.getInstitutionGrowth(14);

            assertThat(result.get(result.size() - 1).date()).isEqualTo(LocalDate.now());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. getTopBorrowedBooks()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTopBorrowedBooks()")
    class GetTopBorrowedBooksTests {

        @Test
        @DisplayName("Should call platform-wide query when institutionId is null")
        void shouldCallPlatformWideQueryWhenInstitutionIdIsNull() {
            when(loanRepository.findTopBorrowedBooks(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            analyticsService.getTopBorrowedBooks(10, null);

            verify(loanRepository).findTopBorrowedBooks(any(Pageable.class));
            verify(loanRepository, never()).findTopBorrowedBooksByInstitution(any(), any());
        }

        @Test
        @DisplayName("Should call institution-filtered query when institutionId is provided")
        void shouldCallFilteredQueryWhenInstitutionIdProvided() {
            when(loanRepository.findTopBorrowedBooksByInstitution(eq(instAId), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            analyticsService.getTopBorrowedBooks(10, instAId);

            verify(loanRepository).findTopBorrowedBooksByInstitution(eq(instAId), any(Pageable.class));
            verify(loanRepository, never()).findTopBorrowedBooks(any());
        }

        @Test
        @DisplayName("Should correctly map Object[] rows to TopBookDTO")
        void shouldCorrectlyMapRowsToDTO() {
            when(loanRepository.findTopBorrowedBooks(any(Pageable.class))).thenReturn(List.of(
                    new Object[]{"BOOK-001", "Clean Code",               "University of Dodoma",    45L},
                    new Object[]{"BOOK-002", "The Pragmatic Programmer", "Dar es Salaam Institute", 30L}
            ));

            List<TopBookDTO> result = analyticsService.getTopBorrowedBooks(10, null);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).publicId()).isEqualTo("BOOK-001");
            assertThat(result.get(0).title()).isEqualTo("Clean Code");
            assertThat(result.get(0).institutionName()).isEqualTo("University of Dodoma");
            assertThat(result.get(0).count()).isEqualTo(45L);
            assertThat(result.get(1).publicId()).isEqualTo("BOOK-002");
            assertThat(result.get(1).count()).isEqualTo(30L);
        }

        @Test
        @DisplayName("Should cap limit at 50 even when caller passes more")
        void shouldCapLimitAtFifty() {
            when(loanRepository.findTopBorrowedBooks(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            analyticsService.getTopBorrowedBooks(999, null);

            verify(loanRepository).findTopBorrowedBooks(
                    argThat(pageable -> pageable.getPageSize() == 50));
        }

        @Test
        @DisplayName("Should return empty list when no loans exist")
        void shouldReturnEmptyListWhenNoLoans() {
            when(loanRepository.findTopBorrowedBooks(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            List<TopBookDTO> result = analyticsService.getTopBorrowedBooks(10, null);

            assertThat(result).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. getTopReservedBooks()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTopReservedBooks()")
    class GetTopReservedBooksTests {

        @Test
        @DisplayName("Should call platform-wide query when institutionId is null")
        void shouldCallPlatformWideQueryWhenNoInstitution() {
            when(reservationRepository.findTopReservedBooks(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            analyticsService.getTopReservedBooks(10, null);

            verify(reservationRepository).findTopReservedBooks(any(Pageable.class));
            verify(reservationRepository, never()).findTopReservedBooksByInstitution(any(), any());
        }

        @Test
        @DisplayName("Should call institution-filtered query when institutionId provided")
        void shouldCallFilteredQueryWhenInstitutionProvided() {
            when(reservationRepository.findTopReservedBooksByInstitution(eq(instBId), any()))
                    .thenReturn(Collections.emptyList());

            analyticsService.getTopReservedBooks(5, instBId);

            verify(reservationRepository).findTopReservedBooksByInstitution(eq(instBId), any());
        }

        @Test
        @DisplayName("Should cap limit at 50")
        void shouldCapLimitAtFifty() {
            when(reservationRepository.findTopReservedBooks(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            analyticsService.getTopReservedBooks(100, null);

            verify(reservationRepository).findTopReservedBooks(
                    argThat(p -> p.getPageSize() == 50));
        }

        @Test
        @DisplayName("Should map result rows to TopBookDTO correctly")
        void shouldMapResultRows() {
            when(reservationRepository.findTopReservedBooks(any(Pageable.class))).thenReturn(List.of(
                    new Object[]{"BOOK-XYZ", "Refactoring", "Some Inst", 15L}
            ));

            List<TopBookDTO> result = analyticsService.getTopReservedBooks(10, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Refactoring");
            assertThat(result.get(0).count()).isEqualTo(15L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. getLeastBorrowedBooks()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getLeastBorrowedBooks()")
    class GetLeastBorrowedBooksTests {

        @Test
        @DisplayName("Should call findLeastBorrowedBooks repository method")
        void shouldCallCorrectRepositoryMethod() {
            when(loanRepository.findLeastBorrowedBooks(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            analyticsService.getLeastBorrowedBooks(10);

            verify(loanRepository).findLeastBorrowedBooks(any(Pageable.class));
        }

        @Test
        @DisplayName("Should cap limit at 50")
        void shouldCapLimit() {
            when(loanRepository.findLeastBorrowedBooks(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            analyticsService.getLeastBorrowedBooks(200);

            verify(loanRepository).findLeastBorrowedBooks(
                    argThat(p -> p.getPageSize() == 50));
        }

        @Test
        @DisplayName("Should correctly map result to TopBookDTO")
        void shouldMapResult() {
            when(loanRepository.findLeastBorrowedBooks(any(Pageable.class))).thenReturn(List.of(
                    new Object[]{"BOOK-LOW", "Rarely Read", "Some Inst", 1L}
            ));

            List<TopBookDTO> result = analyticsService.getLeastBorrowedBooks(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).publicId()).isEqualTo("BOOK-LOW");
            assertThat(result.get(0).count()).isEqualTo(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7 & 8. getLoansTrend() / getReservationsTrend()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getLoansTrend() and getReservationsTrend() — daily gap filling")
    class DailyTrendTests {

        @Test
        @DisplayName("Loans trend: should return days+1 entries")
        void loansTrendShouldReturnCorrectSize() {
            when(loanRepository.countLoansPerDay(any())).thenReturn(Collections.emptyList());

            List<DailyCountDTO> result = analyticsService.getLoansTrend(30);

            assertThat(result).hasSize(31); // 30 days back + today
        }

        @Test
        @DisplayName("Loans trend: zero-count days should be filled in")
        void loansTrendShouldFillZeros() {
            when(loanRepository.countLoansPerDay(any())).thenReturn(Collections.emptyList());

            List<DailyCountDTO> result = analyticsService.getLoansTrend(7);

            assertThat(result).allSatisfy(dto -> assertThat(dto.count()).isZero());
        }

        @Test
        @DisplayName("Loans trend: actual counts from DB should appear on the correct date")
        void loansTrendShouldIncludeActualCounts() {
            LocalDate today = LocalDate.now();
            when(loanRepository.countLoansPerDay(any())).thenReturn(List.of(
                    new Object[]{today, 8L}
            ));

            List<DailyCountDTO> result = analyticsService.getLoansTrend(7);

            Optional<DailyCountDTO> todayEntry =
                    result.stream().filter(d -> d.date().equals(today)).findFirst();
            assertThat(todayEntry).isPresent();
            assertThat(todayEntry.get().count()).isEqualTo(8L);
        }

        @Test
        @DisplayName("Loans trend: dates should be in ascending order")
        void loansTrendDatesAscending() {
            when(loanRepository.countLoansPerDay(any())).thenReturn(Collections.emptyList());

            List<DailyCountDTO> result = analyticsService.getLoansTrend(10);

            for (int i = 0; i < result.size() - 1; i++) {
                assertThat(result.get(i).date()).isBefore(result.get(i + 1).date());
            }
        }

        @Test
        @DisplayName("Reservations trend: should return days+1 entries")
        void reservationsTrendShouldReturnCorrectSize() {
            when(reservationRepository.countReservationsPerDay(any()))
                    .thenReturn(Collections.emptyList());

            List<DailyCountDTO> result = analyticsService.getReservationsTrend(14);

            assertThat(result).hasSize(15);
        }

        @Test
        @DisplayName("Reservations trend: last entry should be today")
        void reservationsTrendLastEntryIsToday() {
            when(reservationRepository.countReservationsPerDay(any()))
                    .thenReturn(Collections.emptyList());

            List<DailyCountDTO> result = analyticsService.getReservationsTrend(5);

            assertThat(result.get(result.size() - 1).date()).isEqualTo(LocalDate.now());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. getLoanStatusDistribution()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getLoanStatusDistribution()")
    class GetLoanStatusDistributionTests {

        @Test
        @DisplayName("Should correctly assign counts to BORROWED, LATE, RETURNED")
        void shouldAssignCountsCorrectly() {
            when(loanRepository.countByStatus()).thenReturn(List.of(
                    new Object[]{LoanStatus.BORROWED, 50L},
                    new Object[]{LoanStatus.LATE,     12L},
                    new Object[]{LoanStatus.RETURNED, 300L}
            ));

            LoanStatusDistributionDTO result = analyticsService.getLoanStatusDistribution();

            assertThat(result.borrowed()).isEqualTo(50L);
            assertThat(result.late()).isEqualTo(12L);
            assertThat(result.returned()).isEqualTo(300L);
        }

        @Test
        @DisplayName("Should return zeros for all statuses when no loans exist")
        void shouldReturnZerosWhenNoLoans() {
            when(loanRepository.countByStatus()).thenReturn(Collections.emptyList());

            LoanStatusDistributionDTO result = analyticsService.getLoanStatusDistribution();

            assertThat(result.borrowed()).isZero();
            assertThat(result.late()).isZero();
            assertThat(result.returned()).isZero();
        }

        @Test
        @DisplayName("Should handle partial results — missing statuses default to zero")
        void shouldHandlePartialResults() {
            when(loanRepository.countByStatus()).thenReturn(List.of(
                    new Object[]{LoanStatus.LATE,     5L},
                    new Object[]{LoanStatus.RETURNED, 100L}
            ));

            LoanStatusDistributionDTO result = analyticsService.getLoanStatusDistribution();

            assertThat(result.borrowed()).isZero();
            assertThat(result.late()).isEqualTo(5L);
            assertThat(result.returned()).isEqualTo(100L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. getDailyActiveUsers()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getDailyActiveUsers()")
    class GetDailyActiveUsersTests {

        @Test
        @DisplayName("Should return days+1 entries")
        void shouldReturnCorrectSize() {
            when(auditLogRepository.countDailyActiveUsers(any())).thenReturn(Collections.emptyList());

            List<DailyCountDTO> result = analyticsService.getDailyActiveUsers(30);

            assertThat(result).hasSize(31);
        }

        @Test
        @DisplayName("Should zero-fill days with no activity")
        void shouldZeroFillMissingDays() {
            when(auditLogRepository.countDailyActiveUsers(any())).thenReturn(Collections.emptyList());

            List<DailyCountDTO> result = analyticsService.getDailyActiveUsers(7);

            assertThat(result).allSatisfy(dto -> assertThat(dto.count()).isZero());
        }

        @Test
        @DisplayName("Should include actual daily user counts from audit log")
        void shouldIncludeActualCounts() {
            LocalDate today = LocalDate.now();
            when(auditLogRepository.countDailyActiveUsers(any())).thenReturn(List.of(
                    new Object[]{today, 7L}
            ));

            List<DailyCountDTO> result = analyticsService.getDailyActiveUsers(7);

            Optional<DailyCountDTO> todayEntry =
                    result.stream().filter(d -> d.date().equals(today)).findFirst();
            assertThat(todayEntry).isPresent();
            assertThat(todayEntry.get().count()).isEqualTo(7L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. getTopActiveUsers()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getTopActiveUsers()")
    class GetTopActiveUsersTests {

        @Test
        @DisplayName("Should map audit log rows to TopActiveUserDTO correctly")
        void shouldMapRowsCorrectly() {
            when(institutionRepository.findAll()).thenReturn(List.of(instA));
            when(auditLogRepository.findTopActiveUsers(any(Pageable.class))).thenReturn(List.of(
                    new Object[]{"librarian@udom.ac.tz", "LIBRARIAN", instAId, 120L}
            ));

            List<TopActiveUserDTO> result = analyticsService.getTopActiveUsers(10);

            assertThat(result).hasSize(1);
            TopActiveUserDTO dto = result.get(0);
            assertThat(dto.actorEmail()).isEqualTo("librarian@udom.ac.tz");
            assertThat(dto.actorRole()).isEqualTo("LIBRARIAN");
            assertThat(dto.institutionName()).isEqualTo("University of Dodoma");
            assertThat(dto.actionCount()).isEqualTo(120L);
        }

        @Test
        @DisplayName("Should show 'Unknown' when institution is not found in lookup")
        void shouldShowUnknownForMissingInstitution() {
            UUID unknownInstId = UUID.randomUUID();
            when(institutionRepository.findAll()).thenReturn(List.of());
            when(auditLogRepository.findTopActiveUsers(any(Pageable.class))).thenReturn(List.of(
                    new Object[]{"ghost@example.com", "ADMIN", unknownInstId, 5L}
            ));

            List<TopActiveUserDTO> result = analyticsService.getTopActiveUsers(10);

            assertThat(result.get(0).institutionName()).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("Should cap limit at 50")
        void shouldCapLimit() {
            when(institutionRepository.findAll()).thenReturn(List.of());
            when(auditLogRepository.findTopActiveUsers(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            analyticsService.getTopActiveUsers(200);

            verify(auditLogRepository).findTopActiveUsers(
                    argThat(p -> p.getPageSize() == 50));
                    //here
        }

        @Test
        @DisplayName("Should return empty list when no audit logs exist")
        void shouldReturnEmptyWhenNoAuditLogs() {
            when(institutionRepository.findAll()).thenReturn(List.of());
            when(auditLogRepository.findTopActiveUsers(any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            List<TopActiveUserDTO> result = analyticsService.getTopActiveUsers(10);

            assertThat(result).isEmpty();
        }
    }

    //
    // 12. getRateLimitViolations()
    //

    @Nested
    @DisplayName("getRateLimitViolations()")
    class GetRateLimitViolationsTests {

        @Test
        @DisplayName("Should return correct counts from Redis key scans")
        void shouldReturnCorrectCounts() {
            when(redisTemplate.keys("rl:LOGIN:*"))
                    .thenReturn(Set.of("rl:LOGIN:1.1.1.1", "rl:LOGIN:2.2.2.2"));
            when(redisTemplate.keys("rl:REGISTER:*"))
                    .thenReturn(Set.of("rl:REGISTER:3.3.3.3"));
            when(redisTemplate.keys("rl:VERIFY:*"))
                    .thenReturn(Set.of());
            when(redisTemplate.keys("rl:FORGOT_PWD:*"))
                    .thenReturn(Set.of("rl:FORGOT_PWD:4.4.4.4"));
            when(redisTemplate.keys("rl:*"))
                    .thenReturn(Set.of(
                            "rl:LOGIN:1.1.1.1", "rl:LOGIN:2.2.2.2",
                            "rl:REGISTER:3.3.3.3",
                            "rl:FORGOT_PWD:4.4.4.4"
                    ));

            RateLimitViolationsDTO result = analyticsService.getRateLimitViolations();

            assertThat(result.blockedOnLogin()).isEqualTo(2L);
            assertThat(result.blockedOnRegister()).isEqualTo(1L);
            assertThat(result.blockedOnVerify()).isZero();
            assertThat(result.blockedOnForgotPassword()).isEqualTo(1L);
            assertThat(result.totalRateLimitKeys()).isEqualTo(4L);
        }

        @Test
        @DisplayName("Should return all zeros when Redis has no rate limit keys")
        void shouldReturnZerosWhenRedisIsEmpty() {
            when(redisTemplate.keys(anyString())).thenReturn(Set.of());

            RateLimitViolationsDTO result = analyticsService.getRateLimitViolations();

            assertThat(result.blockedOnLogin()).isZero();
            assertThat(result.blockedOnRegister()).isZero();
            assertThat(result.blockedOnVerify()).isZero();
            assertThat(result.blockedOnForgotPassword()).isZero();
            assertThat(result.totalRateLimitKeys()).isZero();
        }

        @Test
        @DisplayName("Should return all zeros when Redis returns null (connection issue)")
        void shouldReturnZerosWhenRedisReturnsNull() {
            when(redisTemplate.keys(anyString())).thenReturn(null);

            RateLimitViolationsDTO result = analyticsService.getRateLimitViolations();

            assertThat(result.blockedOnLogin()).isZero();
            assertThat(result.totalRateLimitKeys()).isZero();
        }

        @Test
        @DisplayName("Should return all zeros and not throw when Redis throws exception")
        void shouldHandleRedisExceptionGracefully() {
            when(redisTemplate.keys(anyString()))
                    .thenThrow(new RuntimeException("Redis unavailable"));

            RateLimitViolationsDTO result = analyticsService.getRateLimitViolations();

            assertThat(result.blockedOnLogin()).isZero();

            assertThat(result.blockedOnRegister()).isZero();
            assertThat(result.blockedOnVerify()).isZero();
            assertThat(result.blockedOnForgotPassword()).isZero();
            assertThat(result.totalRateLimitKeys()).isZero();
        }

        @Test
        @DisplayName("Should scan Redis with exactly the right key patterns")
        void shouldScanWithCorrectPatterns() {
            when(redisTemplate.keys(anyString())).thenReturn(Set.of());

            analyticsService.getRateLimitViolations();

            verify(redisTemplate).keys("rl:LOGIN:*");
            verify(redisTemplate).keys("rl:REGISTER:*");
            verify(redisTemplate).keys("rl:VERIFY:*");
            verify(redisTemplate).keys("rl:FORGOT_PWD:*");
            verify(redisTemplate).keys("rl:*");
        }
    }*/
}