package com.saas.libms.system.dto;

import com.saas.libms.institution.Institution;
import com.saas.libms.institution.InstitutionStatus;
import java.time.LocalDateTime;

public record SystemInstitutionResponse(
        String publicId,
        String name,
        String email,
        String phone,
        String address,
        InstitutionStatus status,
        boolean isVerified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SystemInstitutionResponse from(Institution i) {
        return new SystemInstitutionResponse(
                i.getPublicId(),
                i.getName(),
                i.getEmail(),
                i.getPhone(),
                i.getAddress(),
                i.getStatus(),
                i.isVerified(),
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }
}
