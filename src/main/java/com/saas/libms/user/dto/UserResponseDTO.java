package com.saas.libms.user.dto;


import com.saas.libms.user.User;
import com.saas.libms.user.UserRole;
import com.saas.libms.user.UserStatus;

public record UserResponseDTO(
        String publicId,
        String username,
        String email,
        UserRole role,
        UserStatus status,
        String insititutionId
) {
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getInstitution().getPublicId()
        );
    }
}
