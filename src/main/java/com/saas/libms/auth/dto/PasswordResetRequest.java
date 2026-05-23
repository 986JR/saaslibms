package com.saas.libms.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @NotBlank(message = "messages can not be empty")
        @Email(message = "Email Should be in proper format")
        String email
) {
}
