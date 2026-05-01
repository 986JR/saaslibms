package com.saas.libms.security;

import com.saas.libms.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * Everything JWT lives here.
 *
 * Access tokens are short-lived JWTs (5 min).
 * They carry: userId, institutionId, role.
 *
 * Refresh tokens are NOT JWTs — they are random UUIDs stored
 * as HttpOnly cookies and hashed in the database. See AuthService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;


    public String generateAccessToken(UUID userId, UUID institutionId, String role, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessTokenExpiryMs());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("institutionId", institutionId.toString())
                .claim("role", role)
                .claim("email", email)             // so JwtAuthFilter can load user by email
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }


    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT unsupported: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT malformed: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("JWT signature invalid: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT is empty or null: {}", ex.getMessage());
        }
        return false;
    }


    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public UUID extractInstitutionId(String token) {
        return UUID.fromString(parseClaims(token).get("institutionId", String.class));
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }


    public LocalDateTime extractExpiry(String token) {
        Date expiry = parseClaims(token).getExpiration();
        return expiry.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }


    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

