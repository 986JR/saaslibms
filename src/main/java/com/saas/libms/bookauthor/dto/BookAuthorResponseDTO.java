package com.saas.libms.bookauthor.dto;

import com.saas.libms.bookauthor.BookAuthorProjection;

import java.util.UUID;

public record BookAuthorResponseDTO(
        UUID id,
        //from Book
        String bookPublicId,
        String bookTitle,
        String bookIsbn,
        String bookPublisher,
        Integer bookPublishedYear,

        //Author
        String authorPublicId,
        String authorName,
        String authorStatus
) {
    public static BookAuthorResponseDTO from(BookAuthorProjection p) {
        return new BookAuthorResponseDTO(
                p.getId(),
                p.getBookPublicId(),
                p.getBookTitle(),
                p.getBookIsbn(),
                p.getBookPublisher(),
                p.getBookPublishedYear(),
                p.getAuthorPublicId(),
                p.getAuthorName(),
                p.getAuthorStatus()
        );
    }
}
