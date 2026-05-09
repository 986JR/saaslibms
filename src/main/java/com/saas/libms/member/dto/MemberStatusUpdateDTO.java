package com.saas.libms.member.dto;

import com.saas.libms.member.MemberStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberStatusUpdateDTO(
        @NotNull(message = "Status is required")
        MemberStatus status
) {
}
