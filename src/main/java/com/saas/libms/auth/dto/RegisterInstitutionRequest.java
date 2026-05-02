package com.saas.libms.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterInstitutionRequest(
        @NotBlank(message = "Institution name is required")
        @Size(min=2, max = 100, message = "Naame must not be between 2 and 100 character")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Phone number is required")
        String phone,

        @NotBlank(message = "Addres is required")
        String address

) {
}
