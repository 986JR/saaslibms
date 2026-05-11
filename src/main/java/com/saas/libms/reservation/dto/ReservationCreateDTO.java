package com.saas.libms.reservation.dto;

import jakarta.validation.constraints.NotBlank;

public record ReservationCreateDTO(
        @NotBlank(message = "Member public ID is required")
        String memberPublicId,

        @NotBlank(message = "Book public ID is required")
        String  bookPublicId
) {
}
