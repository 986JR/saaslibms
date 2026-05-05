package com.saas.libms.book.dto;

import com.saas.libms.book.Book;

import java.time.LocalDateTime;

public record BookResponseDTO(
        String publicId,
        String title,
        String isbn,
        String publisher,
        Integer publishedYear,
        int copiesTotal,
        int copiesAvailable,
        String institutionId,
        LocalDateTime createdAt
    ) {

    public static BookResponseDTO from(Book book) {
        return new BookResponseDTO(
                book.getPublicId(),
                book.getTitle(),
                book.getIsbn(),
                book.getPublisher(),
                book.getPublishedYear(),
                book.getCopiesTotal(),
                book.getCopiesAvailable(),
                book.getInstitution().getPublicId(),
                book.getCreatedAt()
        );
    }
}
