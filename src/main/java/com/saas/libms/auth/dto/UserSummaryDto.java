package com.saas.libms.auth.dto;

public record UserSummaryDto(
        String publicId,
        String Username,
        String email,
        String role,
        String institutionName
) {
}
