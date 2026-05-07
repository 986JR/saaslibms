package com.saas.libms.bookauthor.dto;

import jakarta.validation.constraints.NotBlank;

public record BookAuthorCreateDTO(
        @NotBlank(message = "Book public ID is required")
        String bookPublicId,

        @NotBlank(message = "Author public ID is required")
        String authorPublicId
) {
}
