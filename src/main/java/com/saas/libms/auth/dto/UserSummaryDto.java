package com.saas.libms.auth.dto;

import com.saas.libms.institution.Institution;
import com.saas.libms.user.UserRole;

import java.util.UUID;

public record UserSummaryDto(
        UUID publicId,
        String Username,
        String email,
        String role,
        Institution institutionId
) {
}
