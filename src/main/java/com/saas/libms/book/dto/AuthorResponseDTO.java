package com.saas.libms.book.dto;

import com.saas.libms.book.Author;

public record AuthorResponseDTO(
        String publicId,
        String name,
        String status,
        String institutionId
) {
    public static AuthorResponseDTO from(Author author) {
        return new AuthorResponseDTO(
                author.getPublicId(),
                author.getName(),
                author.getStatus().name(),
                author.getInstitution().getPublicId()

        );
    }
}
