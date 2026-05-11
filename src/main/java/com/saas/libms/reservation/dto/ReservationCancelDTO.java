package com.saas.libms.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReservationCancelDTO(
        @NotBlank(message = "Cancel reason is required")
        @Size(max = 255, message = "Cancel reason must not exceed 255 characters")
        String reason
) {
}
