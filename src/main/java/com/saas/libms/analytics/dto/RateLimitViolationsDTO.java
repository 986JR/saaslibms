package com.saas.libms.analytics.dto;
/**
 * Rate limit violation snapshot — how many clients are currently
 * blocked per sensitive endpoint.
 *
 * This is derived from the live Redis rate limit sorted sets, not
 * from a database table — it reflects the CURRENT window only.
 *
 * Returned by GET /api/v1/system/analytics/traffic/rate-limit-violations
 */
public record RateLimitViolationsDTO(
        long blockedOnLogin,
        long blockedOnRegister,
        long blockedOnVerify,
        long blockedOnForgotPassword,
        long totalRateLimitKeys    // total rl:* keys in Redis right now
) {}

