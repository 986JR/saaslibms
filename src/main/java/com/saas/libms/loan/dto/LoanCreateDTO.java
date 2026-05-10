package com.saas.libms.loan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record LoanCreateDTO(
        @NotBlank(message = "Member public ID is Required")
        String memberPublicId,

        @NotBlank(message = "Book public ID is required")
        String bookPublicId,

        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}
