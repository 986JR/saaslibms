package com.saas.libms.auth.passwordreset;

import com.saas.libms.auth.passwordreset.dto.ForgotPasswordRequest;
import com.saas.libms.auth.passwordreset.dto.ResetPasswordRequest;
import com.saas.libms.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPAssword(
            @Valid @RequestBody ForgotPasswordRequest request
            ) {
        passwordResetService.requestPasswordReset(request);

        return ResponseEntity.ok(ApiResponse.success(
                "If that email is registered, a reset link has been sent. Check your inbox", null
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request
            ) {
        passwordResetService.resetPassword(token,request);
        return ResponseEntity.ok(ApiResponse.success(
                "Your password has been reset successfully. You can now log in with your new password.", null
        ));
    }
}
