package com.saas.libms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetUpAdminRequest(
        @NotBlank(message = "Institution public Id ID is required")
        String institutionPublicId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be atleast 8 characters")
        String password,

        @NotBlank(message = "Please Confirm your Password")
        String confirmPassword
) {
}
