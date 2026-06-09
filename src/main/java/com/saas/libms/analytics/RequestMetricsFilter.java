package com.saas.libms.analytics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestMetricsFilter extends OncePerRequestFilter {


    private final RequestMetricsWriterService writerService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long startMs = System.currentTimeMillis();

        // Let the request proceed through the entire chain first
        filterChain.doFilter(request, response);

        // After response is committed — record metrics asynchronously
        long durationMs = System.currentTimeMillis() - startMs;
        int statusCode = response.getStatus();
        String endpoint = normalizeEndpoint(request.getMethod(), request.getRequestURI());
        String clientIp = extractIp(request);
        UUID institutionId = extractInstitutionId();

        // Fire and forget — must not throw
        writerService.write(endpoint, statusCode, durationMs, institutionId, clientIp);
    }

    /**
     * Normalize path variables so the endpoint column has low cardinality.
     *
     * Rules (applied in order):
     *   1. Segments matching a public ID pattern (e.g. BOOK-K3MP9R, INST-A1B2C3) → {id}
     *   2. Segments matching a UUID → {id}
     *   3. Query strings stripped (already not in getRequestURI)
     *
     * Examples:
     *   GET  /api/v1/books/BOOK-K3MP9R            → GET /api/v1/books/{id}
     *   GET  /api/v1/members/MBR-XYZ/loans        → GET /api/v1/members/{id}/loans
     *   POST /api/v1/auth/login                   → POST /api/v1/auth/login  (unchanged)
     */
    private String normalizeEndpoint(String method, String uri) {
        // Replace public ID segments: uppercase letters + hyphen + alphanumerics
        // e.g. BOOK-K3MP9R, INST-A1B2, USR-XYZ123
        String normalized = uri.replaceAll("/[A-Z]+-[A-Z0-9]+", "/{id}");

        // Replace UUID segments
        normalized = normalized.replaceAll(
                "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "/{id}");

        // Truncate to 120 chars to match column length (very long URIs are anomalies)
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 117) + "...";
        }

        return method + " " + normalized;
    }

    /**
     * Extract institution ID from the SecurityContext if the user is authenticated
     * and is not a SUPER_ADMIN (who has no institution).
     */
    private UUID extractInstitutionId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;

            Object principal = auth.getPrincipal();
            if (principal instanceof com.saas.libms.security.CustomUserDetails userDetails) {
                var user = userDetails.getUser();
                if (user.getInstitution() != null) {
                    return user.getInstitution().getId();
                }
            }
        } catch (Exception e) {
            // Institution ID is optional — don't let extraction failure matter
            log.debug("Could not extract institutionId from SecurityContext: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract client IP — same logic as RateLimitFilter for consistency.
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Skip recording for Spring Boot Actuator and static resource paths.
     * These don't need traffic analytics.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator")
                || uri.startsWith("/favicon")
                || uri.startsWith("/error");
    }

}
