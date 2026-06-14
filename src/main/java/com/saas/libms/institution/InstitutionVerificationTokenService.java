package com.saas.libms.institution;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstitutionVerificationTokenService {
    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", ""); // 32-char hex token
    }

    public boolean isTokenExpired(LocalDateTime expiresAt) {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
