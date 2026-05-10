package com.saas.libms.loan.dto;

import jakarta.validation.constraints.Min;

public record LoanReturnDTO(
        @Min(value = 1, message =   "Return quantity must be at least 1")
        Integer quantity
) {
}
