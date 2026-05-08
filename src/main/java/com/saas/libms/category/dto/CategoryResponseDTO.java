package com.saas.libms.category.dto;

import com.saas.libms.category.Category;

public record CategoryResponseDTO(
        String name,
        String institution,
        String publicId
) {
    public static CategoryResponseDTO from(Category category) {
        return new CategoryResponseDTO(
                category.getName(),
                category.getInstitution().getName(),
                category.getPublicId()
        );
    }
}
