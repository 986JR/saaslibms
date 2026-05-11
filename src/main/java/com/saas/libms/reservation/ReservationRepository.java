package com.saas.libms.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    // ─── Single Reservation Lookup

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.institution
            JOIN FETCH r.book
            JOIN FETCH r.member
            WHERE r.publicId = :publicId
              AND r.institution.id = :institutionId
              AND r.archived = false
            """)
    Optional<Reservation> findByPublicIdAndInstitutionId(
            @Param("publicId") String publicId,
            @Param("institutionId") UUID institutionId
    );

    // ─── Paginated List with Optional Filters

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.book
            JOIN FETCH r.member
            WHERE r.institution.id = :institutionId
              AND r.archived = false
              AND (:status IS NULL OR r.status = :status)
              AND (:memberPublicId IS NULL OR r.member.publicId = :memberPublicId)
              AND (:bookPublicId IS NULL OR r.book.publicId = :bookPublicId)
            ORDER BY r.createdAt DESC
            """)
    Page<Reservation> findAllByInstitutionId(
            @Param("institutionId") UUID institutionId,
            @Param("status") ReservationStatus status,
            @Param("memberPublicId") String memberPublicId,
            @Param("bookPublicId") String bookPublicId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.institution.id = :institutionId
              AND r.archived = false
              AND (:status IS NULL OR r.status = :status)
              AND (:memberPublicId IS NULL OR r.member.publicId = :memberPublicId)
              AND (:bookPublicId IS NULL OR r.book.publicId = :bookPublicId)
            """)
    long countAllByInstitutionId(
            @Param("institutionId") UUID institutionId,
            @Param("status") ReservationStatus status,
            @Param("memberPublicId") String memberPublicId,
            @Param("bookPublicId") String bookPublicId
    );

    //Duplicate / Active Reservation Check
    // Prevents the same member reserving the same book while PENDING or FULFILLED

    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.member.id = :memberId
              AND r.book.id = :bookId
              AND r.status IN (com.saas.libms.reservation.ReservationStatus.PENDING,
                               com.saas.libms.reservation.ReservationStatus.FULFILLED)
              AND r.archived = false
            """)
    boolean existsActiveReservation(
            @Param("memberId") UUID memberId,
            @Param("bookId") UUID bookId
    );

    // Scheduler: Find Pending Reservations for a Book (ordered by queue) ──

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.book
            JOIN FETCH r.member
            WHERE r.book.id = :bookId
              AND r.status = com.saas.libms.reservation.ReservationStatus.PENDING
              AND r.archived = false
            ORDER BY r.queuePosition ASC
            """)
    List<Reservation> findPendingReservationsForBook(@Param("bookId") UUID bookId);

    //Scheduler: Find Fulfilled Reservations That Have Expired

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.book
            JOIN FETCH r.member
            WHERE r.status = com.saas.libms.reservation.ReservationStatus.FULFILLED
              AND r.reservedUntil < :now
              AND r.archived = false
            """)
    List<Reservation> findFulfilledReservationsToExpire(@Param("now") LocalDateTime now);

    //  Count Pending Reservations for Queue Position Calculation

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.book.id = :bookId
              AND r.status = com.saas.libms.reservation.ReservationStatus.PENDING
              AND r.archived = false
            """)
    int countPendingReservationsForBook(@Param("bookId") UUID bookId);

    // Scheduler: Find All Books That Have Pending Reservations
    // Used to know which books to check for available copies

    @Query("""
            SELECT DISTINCT r.book.id FROM Reservation r
            WHERE r.status = com.saas.libms.reservation.ReservationStatus.PENDING
              AND r.archived = false
            """)
    List<UUID> findBookIdsWithPendingReservations();

    //Member's Reservations for Active Loan Guard

    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.member.id = :memberId
              AND r.book.id = :bookId
              AND r.status IN (com.saas.libms.reservation.ReservationStatus.PENDING,
                               com.saas.libms.reservation.ReservationStatus.FULFILLED)
              AND r.archived = false
            """)
    boolean hasPendingOrFulfilledReservation(
            @Param("memberId") UUID memberId,
            @Param("bookId") UUID bookId
    );

    // Count Active Reservations for Member (for reservation limit check)

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.member.id = :memberId
              AND r.status IN (com.saas.libms.reservation.ReservationStatus.PENDING,
                               com.saas.libms.reservation.ReservationStatus.FULFILLED)
              AND r.archived = false
            """)
    int countActiveReservationsByMember(@Param("memberId") UUID memberId);







}
