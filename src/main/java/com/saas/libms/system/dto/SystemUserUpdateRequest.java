package com.saas.libms.system.dto;

import com.saas.libms.user.UserRole;
import com.saas.libms.user.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SystemUserUpdateRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Role is required")
        UserRole role,

        @NotNull(message = "Status is required")
        UserStatus status,

        String institutionPublicId
) {}
