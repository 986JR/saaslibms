package com.saas.libms.analytics;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookViewEventRepository extends JpaRepository<BookViewEvent, UUID> {

    /**
     * Most viewed books platform-wide.
     * Returns [bookPublicId, bookTitle, institutionId, viewCount].
     */
    @Query("""
        SELECT b.bookPublicId, b.bookTitle, b.institutionId, COUNT(b)
        FROM BookViewEvent b
        WHERE b.viewedAt >= :from
        GROUP BY b.bookPublicId, b.bookTitle, b.institutionId
        ORDER BY COUNT(b) DESC
        """)
    List<Object[]> findTopViewedBooks(@Param("from") LocalDateTime from, Pageable pageable);

    /**
     * Most viewed books filtered by institution.
     */
    @Query("""
        SELECT b.bookPublicId, b.bookTitle, b.institutionId, COUNT(b)
        FROM BookViewEvent b
        WHERE b.institutionId = :institutionId
          AND b.viewedAt >= :from
        GROUP BY b.bookPublicId, b.bookTitle, b.institutionId
        ORDER BY COUNT(b) DESC
        """)
    List<Object[]> findTopViewedBooksByInstitution(
            @Param("institutionId") UUID institutionId,
            @Param("from") LocalDateTime from,
            Pageable pageable);

    /**
     * Book views per day — for the views trend line chart.
     * Returns [LocalDate, count].
     */
    @Query("""
        SELECT CAST(b.viewedAt AS LocalDate), COUNT(b)
        FROM BookViewEvent b
        WHERE b.viewedAt >= :from
        GROUP BY CAST(b.viewedAt AS LocalDate)
        ORDER BY CAST(b.viewedAt AS LocalDate) ASC
        """)
    List<Object[]> countViewsPerDay(@Param("from") LocalDateTime from);

    /**
     * Delete events older than the given cutoff — for the cleanup scheduler.
     */
    @Modifying
    @Query("DELETE FROM BookViewEvent b WHERE b.viewedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
