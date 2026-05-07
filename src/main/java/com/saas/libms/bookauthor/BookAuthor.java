package com.saas.libms.bookauthor;

import com.saas.libms.author.Author;
import com.saas.libms.book.Book;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "book_authors",
        uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "author_id"}),
        indexes = {
                @Index(name = "idx_book_authors_book", columnList = "book_id"),
                @Index(name = "idx_book_authors_author", columnList = "author_id")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;
}
