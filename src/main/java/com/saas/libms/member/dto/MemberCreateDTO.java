package com.saas.libms.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberCreateDTO(
        @NotBlank(message = "Full Name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @Email(message = "Email must be a valid address")
        @Size(max = 127, message = "Email must nit exceed 127 characters")
        String email,

        @Size(max = 10, message = "Phone must not exceed 10 characters")
        String phone,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address
) {
}
