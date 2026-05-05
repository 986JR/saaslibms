package com.saas.libms.book.dto;

import jakarta.validation.constraints.Min;

public record BookUpdateDTO(
        String title,
        String isbn,
        String publisher,
        Integer publishedYear,
        @Min(value = 1, message = "Total copies must be at least 1")
        Integer copiesTotal
) {
}
