package com.saas.libms.member.dto;

import com.saas.libms.member.Member;
import com.saas.libms.member.MemberStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResponseDTO(
        String publicId,
        String fullName,
        String email,
        String phone,
        MemberStatus status,
        String institutionId,
        LocalDateTime createdAt
) {
    public static MemberResponseDTO from(Member member) {
        return new MemberResponseDTO(
                member.getPublicId(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getStatus(),
                member.getInstitution().getPublicId(),
                member.getCreatedAt()
        );
    }
}
