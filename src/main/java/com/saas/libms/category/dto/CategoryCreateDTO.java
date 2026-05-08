package com.saas.libms.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryCreateDTO(
        @NotBlank(message = "Category name is required")
        String name
) {
}
