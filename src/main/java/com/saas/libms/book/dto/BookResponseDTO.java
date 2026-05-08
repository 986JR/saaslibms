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
        String categoryName,
        LocalDateTime createdAt
    ) {

    public static BookResponseDTO from(Book book) {
        String categoryName = null;

        if (book.getCategory() != null) {
            categoryName = book.getCategory().getName();
        }
        return new BookResponseDTO(
                book.getPublicId(),
                book.getTitle(),
                book.getIsbn(),
                book.getPublisher(),
                book.getPublishedYear(),
                book.getCopiesTotal(),
                book.getCopiesAvailable(),
                book.getInstitution().getPublicId(),
                categoryName,
                book.getCreatedAt()
        );
    }
}
