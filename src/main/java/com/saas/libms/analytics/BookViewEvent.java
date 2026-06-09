package com.saas.libms.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "book_view_events",
        indexes = {
                @Index(name = "idx_book_view_book_id",        columnList = "book_id"),
                @Index(name = "idx_book_view_institution_id", columnList = "institution_id"),
                @Index(name = "idx_book_view_viewed_at",      columnList = "viewed_at")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookViewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The book that was viewed. Plain UUID column — no FK constraint.
     * This matches the pattern used in AuditLog where references are stored
     * as plain UUIDs to avoid cascading delete issues.
     */
    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    /**
     * Book's public ID at time of view — denormalized for easy reporting
     * without joining back to the books table.
     */
    @Column(name = "book_public_id", nullable = false, length = 30)
    private String bookPublicId;

    /**
     * Book title at time of view — denormalized so reports still work
     * if the book title changes or the book is deleted later.
     */
    @Column(name = "book_title", nullable = false, length = 300)
    private String bookTitle;

    /**
     * Institution that owns the book.
     */
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @CreationTimestamp
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private LocalDateTime viewedAt;

}
