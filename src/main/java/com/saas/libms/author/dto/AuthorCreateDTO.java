package com.saas.libms.author.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorCreateDTO(
        @NotBlank(message = "Author name is required")
        String name
) {
}
