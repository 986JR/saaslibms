package com.saas.libms.auth.dto;

public record PasswordResetResponse(
        String message
) {
    public static PasswordResetResponse of(String message){
        return new PasswordResetResponse(message);
    }
}
