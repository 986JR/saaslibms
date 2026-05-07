package com.saas.libms.bookauthor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, UUID> {
    @Query("SELECT COUNT(ba) > 0 FROM BookAuthor ba WHERE ba.book.id = :bookId ANd ba.author.id = :authorId")
    boolean existsByBookIdAndAuthorId(
            @Param("bookId") UUID bookId,
            @Param("authorId") UUID authorId
    );

    @Modifying
    @Query(value = "DELETE FROM book_authors where author_id = :authorId", nativeQuery = true)
    void deleteByAuthorId(@Param("authorId") UUID authorId);

    //For internal use
    @Modifying
    @Query(value = "DELETE FROM book_authors WHERE book_id = :bookId AND author_id = :auhtorId", nativeQuery = true)
    void deleteByBookIdAndAuthorId(@Param("bookId") UUID bookId,
                                   @Param("authorId") UUID authorId);


    @Query(value = """
            SELECT
                ba.id            AS id,
                b.public_id      AS bookPublicId,
                b.title          AS bookTitle,
                b.isbn           AS bookIsbn,
                b.publisher      AS bookPublisher,
                b.published_year AS bookPublishedYear,
                a.public_id      AS authorPublicId,
                a.name           AS authorName,
                a.author_status  AS authorStatus
            FROM book_authors ba
            JOIN books   b ON b.id = ba.book_id
            JOIN authors a ON a.id = ba.author_id
            WHERE a.public_id      = :authorPublicId
              AND b.institution_id = :institutionId
            """, nativeQuery = true)
    List<BookAuthorProjection> findByAuthorPublicId(@Param("authorPublicId") String authorPublicId,
                                                @Param("institutionId") UUID institutionId);


    @Query(value = """
            SELECT
                ba.id            AS id,
                b.public_id      AS bookPublicId,
                b.title          AS bookTitle,
                b.isbn           AS bookIsbn,
                b.publisher      AS bookPublisher,
                b.published_year AS bookPublishedYear,
                a.public_id      AS authorPublicId,
                a.name           AS authorName,
                a.author_status  AS authorStatus
            FROM book_authors ba
            JOIN books   b ON b.id = ba.book_id
            JOIN authors a ON a.id = ba.author_id
            WHERE b.public_id      = :bookPublicId
              AND b.institution_id = :institutionId
            """, nativeQuery = true)
    List<BookAuthorProjection> findByBookPublicId(@Param("bookPublicId") String bookPublicId,
                                                  @Param("institutionId") UUID institutionId);


}
