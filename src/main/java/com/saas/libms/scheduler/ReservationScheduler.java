package com.saas.libms.scheduler;

import com.saas.libms.book.Book;
import com.saas.libms.book.BookRepository;
import com.saas.libms.common.EmailService;
import com.saas.libms.reservation.Reservation;
import com.saas.libms.reservation.ReservationRepository;
import com.saas.libms.reservation.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ReservationScheduler runs two jobs:
 *
 * 1. processReservations()  — every 5 minutes
 *    Finds books that now have available copies and fulfills the next
 *    queued PENDING reservation for each of those books.
 *
 * 2. expireReservations()   — every 1 hour
 *    Finds FULFILLED reservations whose reservedUntil has passed
 *    (member did not collect in time), marks them EXPIRED, and
 *    restores the copy back to the book.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final EmailService emailService;

    // Member has 48 hours to collect once fulfilled
    private static final int COLLECTION_WINDOW_HOURS = 48;

    // ─── FULFILLMENT SCHEDULER ───────────────────────────────────────────────
    // Runs every 5 minutes. Checks if any books with pending reservations now
    // have available copies, and if so, fulfills the next member in queue.

    @Scheduled(fixedRate = 300_000) // 5 minutes
    @Transactional
    public void processReservations() {
        log.debug("Running reservation fulfillment scheduler...");

        // Find every book that has at least one PENDING reservation
        List<UUID> bookIds = reservationRepository.findBookIdsWithPendingReservations();

        if (bookIds.isEmpty()) {
            log.debug("No pending reservations found. Scheduler done.");
            return;
        }

        int fulfilled = 0;

        for (UUID bookId : bookIds) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null) continue;

            // No copies available for this book — skip, nothing to fulfill yet
            if (book.getCopiesAvailable() <= 0) {
                continue;
            }

            // Get the queue of pending reservations for this book, ordered by queue position
            List<Reservation> pending = reservationRepository.findPendingReservationsForBook(bookId);

            for (Reservation reservation : pending) {
                // Double-check copies before each fulfillment (they may run out mid-loop)
                if (book.getCopiesAvailable() <= 0) {
                    break;
                }

                // Fulfill the reservation
                LocalDateTime now = LocalDateTime.now();
                reservation.setStatus(ReservationStatus.FULFILLED);
                reservation.setFulfilledAt(now);
                reservation.setReservedUntil(now.plusHours(COLLECTION_WINDOW_HOURS));
                reservation.setUpdatedAt(now);
                reservationRepository.save(reservation);

                // Reduce available copies — this copy is now reserved for this member
                book.setCopiesAvailable(book.getCopiesAvailable() - 1);
                bookRepository.save(book);

                // Notify the member by email
                emailService.sendReservationFulfilledEmail(
                        reservation.getMember().getEmail(),
                        reservation.getMember().getName(),
                        reservation.getBook().getTitle(),
                        reservation.getPublicId(),
                        reservation.getReservedUntil()
                );

                log.info("Reservation {} fulfilled for member {} — book '{}'. Copies now available: {}",
                        reservation.getPublicId(),
                        reservation.getMember().getPublicId(),
                        book.getTitle(),
                        book.getCopiesAvailable());

                fulfilled++;
            }
        }

        if (fulfilled > 0) {
            log.info("Fulfillment scheduler complete. {} reservation(s) fulfilled.", fulfilled);
        }
    }

    // ─── EXPIRY SCHEDULER ────────────────────────────────────────────────────
    // Runs every hour. Finds FULFILLED reservations where the member did not
    // collect within the 48-hour window, marks them EXPIRED, and restores
    // the copy back to the book so the next member in queue can get it.

    @Scheduled(fixedRate = 3_600_000) // 1 hour
    @Transactional
    public void expireReservations() {
        log.debug("Running reservation expiry scheduler...");

        LocalDateTime now = LocalDateTime.now();
        List<Reservation> toExpire = reservationRepository.findFulfilledReservationsToExpire(now);

        if (toExpire.isEmpty()) {
            log.debug("No fulfilled reservations to expire. Scheduler done.");
            return;
        }

        for (Reservation reservation : toExpire) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setUpdatedAt(now);
            reservationRepository.save(reservation);

            // Restore the copy — member did not collect, so it's available again
            Book book = reservation.getBook();
            int restored = Math.min(book.getCopiesAvailable() + 1, book.getCopiesTotal());
            book.setCopiesAvailable(restored);
            bookRepository.save(book);

            // Notify the member that their reservation has expired
            emailService.sendReservationExpiredEmail(
                    reservation.getMember().getEmail(),
                    reservation.getMember().getName(),
                    reservation.getBook().getTitle(),
                    reservation.getPublicId()
            );

            log.info("Reservation {} expired for member {} — book '{}'. Copy restored. Available now: {}",
                    reservation.getPublicId(),
                    reservation.getMember().getPublicId(),
                    book.getTitle(),
                    book.getCopiesAvailable());
        }

        log.info("Expiry scheduler complete. {} reservation(s) expired.", toExpire.size());
    }
}