package com.saas.libms.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.libms.common.ApiResponse;
import com.saas.libms.config.RateLimitConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Rate limiting filter — runs ONCE per request, before JwtAuthFilter.
 *
 * Order of checks per request:
 *   1. Extract client key (IP + optional device signature)
 *   2. Check global limit  (Tier 1 — all endpoints)
 *   3. Check endpoint-specific limit if the path matches (Tier 2)
 *   4. If either check blocks → return 429 with ApiResponse body
 *   5. If both pass → set informational headers and continue
 *
 * 429 responses use the same ApiResponse envelope as the rest of the API
 * so the frontend handles them uniformly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientKey = buildClientKey(request);
        String path = request.getRequestURI();

        // ── Tier 1: Global limit ─────────────────────────────────────────────
        RateLimitResult globalResult = rateLimitService.check(
                RateLimitConfig.PREFIX_GLOBAL,
                clientKey,
                RateLimitConfig.GLOBAL_LIMIT,
                RateLimitConfig.GLOBAL_WINDOW
        );

        if (!globalResult.allowed()) {
            sendTooManyRequestsResponse(response, globalResult,
                    "Too many requests. Please slow down.");
            return;
        }

        // ── Tier 2: Endpoint-specific limits ─────────────────────────────────
        RateLimitResult endpointResult = checkEndpointLimit(path, clientKey);

        if (endpointResult != null && !endpointResult.allowed()) {
            sendTooManyRequestsResponse(response, endpointResult,
                    endpointMessage(path));
            return;
        }

        // ── Both passed: set rate limit headers and continue ─────────────────
        // Use the more restrictive of the two results for the headers
        RateLimitResult headerResult = (endpointResult != null) ? endpointResult : globalResult;
        setRateLimitHeaders(response, headerResult);

        filterChain.doFilter(request, response);
    }

    /**
     * Build a client key from IP + device signature.
     *
     * IP alone is used as fallback. Device signature adds User-Agent and
     * Accept-Language to reduce false positives on shared IPs (e.g. university
     * networks where many students share one public IP).
     *
     * The signature does NOT stop a determined attacker (they can spoof headers)
     * but it stops casual abuse and reduces collateral damage on shared IPs.
     */
    private String buildClientKey(HttpServletRequest request) {
        String ip = extractIp(request);
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");

        // Only include signature components if they are present
        // Avoids creating weirdly long keys for clients with no headers
        if (userAgent != null && !userAgent.isBlank()) {
            // Truncate to first 50 chars — enough to differentiate browsers,
            // short enough to keep Redis keys reasonable
            String shortAgent = userAgent.length() > 50
                    ? userAgent.substring(0, 50)
                    : userAgent;
            String lang = (acceptLanguage != null && acceptLanguage.length() > 10)
                    ? acceptLanguage.substring(0, 10)
                    : (acceptLanguage != null ? acceptLanguage : "");

            return ip + "|" + shortAgent + "|" + lang;
        }

        return ip;
    }

    /**
     * Extract the real client IP.
     * Handles reverse proxies (Nginx, AWS ELB, Cloudflare) via X-Forwarded-For.
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be a comma-separated list: "clientIP, proxy1, proxy2"
            // The first entry is the original client IP
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Check endpoint-specific limits.
     * Returns null if this path has no special rule (global limit is sufficient).
     */
    private RateLimitResult checkEndpointLimit(String path, String clientKey) {
        if (path.equals("/api/v1/auth/login")) {
            return rateLimitService.check(
                    RateLimitConfig.PREFIX_LOGIN, clientKey,
                    RateLimitConfig.LOGIN_LIMIT, RateLimitConfig.LOGIN_WINDOW);
        }
        if (path.equals("/api/v1/auth/register")) {
            return rateLimitService.check(
                    RateLimitConfig.PREFIX_REGISTER, clientKey,
                    RateLimitConfig.REGISTER_LIMIT, RateLimitConfig.REGISTER_WINDOW);
        }
        if (path.equals("/api/v1/auth/verify")) {
            return rateLimitService.check(
                    RateLimitConfig.PREFIX_VERIFY, clientKey,
                    RateLimitConfig.VERIFY_LIMIT, RateLimitConfig.VERIFY_WINDOW);
        }
        if (path.equals("/api/v1/auth/forgot-password")) {
            return rateLimitService.check(
                    RateLimitConfig.PREFIX_FORGOT_PASSWORD, clientKey,
                    RateLimitConfig.FORGOT_PASSWORD_LIMIT, RateLimitConfig.FORGOT_PASSWORD_WINDOW);
        }
        return null; // no special rule for this path
    }

    /**
     * Write a 429 response using the same ApiResponse envelope as the rest of the API.
     */
    private void sendTooManyRequestsResponse(HttpServletResponse response,
                                             RateLimitResult result,
                                             String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Retry-After tells the client exactly how many seconds to wait
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", "0");

        ApiResponse<Void> body = ApiResponse.error(message);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Set informational rate limit headers on allowed responses.
     * These let the frontend show a warning before the limit is hit.
     */
    private void setRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset",
                String.valueOf(System.currentTimeMillis() + result.resetAfterMs()));
    }

    /**
     * Human-readable 429 message per endpoint.
     */
    private String endpointMessage(String path) {
        return switch (path) {
            case "/api/v1/auth/login" ->
                    "Too many login attempts. Please wait 15 minutes before trying again.";
            case "/api/v1/auth/register" ->
                    "Too many registration attempts from this device. Please try again later.";
            case "/api/v1/auth/verify" ->
                    "Too many verification attempts. Please wait before trying again.";
            case "/api/v1/auth/forgot-password" ->
                    "Too many password reset requests. Please wait 15 minutes before trying again.";
            default ->
                    "Too many requests. Please try again later.";
        };
    }
}