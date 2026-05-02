package com.saas.libms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyInstitutionRequest(
        @NotBlank(message = "Institution public ID is required")
        String InstitutionPublicId,

        @NotBlank(message = "Verification code is required")
        @Size(min = 6, message = "Verififcaation code must be exactly 6 characters")
        String VerificationCode
) {
}
