package com.saas.libms.auth.dto;

import jakarta.validation.constraints.NotBlank;


public record VerifyDnsTxtRequest(
        @NotBlank(message = "Institution public ID is required")
        String institutionPublicId
) {}
