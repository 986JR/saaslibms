package com.saas.libms.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookCreateDTO(
        @NotBlank(message = "Title is required")
        String title,

        String isbn,

        String publisher,

        Integer publishedYear,

        @NotNull(message = "Total copies is required")
        @Min(value = 1, message = "Total copies must be aat least 1")
        Integer copiesTotal


) {
}
