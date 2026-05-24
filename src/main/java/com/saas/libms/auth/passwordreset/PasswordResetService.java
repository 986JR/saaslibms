package com.saas.libms.auth.passwordreset;

import com.saas.libms.auth.passwordreset.dto.ForgotPasswordRequest;
import com.saas.libms.auth.passwordreset.dto.ResetPasswordRequest;
import com.saas.libms.common.EmailService;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.BadRequestException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.security.TokenHashUtil;
import com.saas.libms.user.User;
import com.saas.libms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    //private final PublicIdGenerator publicIdGenerator;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUl;

    @Value("${app.password-reset.expiry-minutes:5}")
    private int tokenExpiryMinutes;

    //request
    @Transactional
    public  void requestPasswordReset(ForgotPasswordRequest request) {

        String email = request.email();

        userRepository.findByEmail( email).ifPresent(user -> {
            try {
                String rawToken = PublicIdGenerator.generateVerificationCode();

                String tokenHash = TokenHashUtil.hash(rawToken);

                passwordResetTokenRepository.deleteAllByEmail(email);

                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .email(email)
                        .tokenHash(tokenHash)
                        .expiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes))
                        .used(false)
                        .build();

                passwordResetTokenRepository.save(resetToken);

                //build reset link
                String resetLink = baseUl+"/api/v1/auth/reset-password?token=" + rawToken;

                emailService.sendPasswordResetEmail(email, user.getUsername(),
                        resetLink, rawToken, tokenExpiryMinutes);

                log.info("Password reset token generated for user: {}", email);
            }
            catch (Exception e) {
                log.error("Failed to generate password reset token for {}: {}", email, e.getMessage());
            }
        });

        if (userRepository.findByEmail(email).isEmpty()) {
            log.warn("Password reset requested for unknown email: {}", email);
        }
    }


    @Transactional
    public void resetPassword(String rawToken, ResetPasswordRequest request) {
        if(!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        String tokenHash = TokenHashUtil.hash(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(()-> new BadRequestException("Invalid or expired reset token"));

        //Check if the token has expired
        if(resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This reset link has expired. Please request a new one.");
        }

        //check if token used
        if(resetToken.isUsed()) {
            throw new BadRequestException("This reset link has already been used. Please request a new one.");
        }

        //Find a user by email
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(()-> new ResourceNotFoundException("User account not found"));

        //Update the password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password successfully reset for user: {}", resetToken.getEmail());

    }

}
