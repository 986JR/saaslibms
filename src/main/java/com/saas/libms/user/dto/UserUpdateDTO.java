package com.saas.libms.user.dto;

import com.saas.libms.user.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
        String username,

        @Email(message = "Email must be valid")
        String email,

        @Size(min = 8, message = "Passowrd must be at least 8 characters" )
        String password,

        //Admin only
        UserStatus status
) {
}
