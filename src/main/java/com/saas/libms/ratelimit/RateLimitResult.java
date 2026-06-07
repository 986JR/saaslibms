package com.saas.libms.ratelimit;

public record RateLimitResult(
        boolean allowed,
        int limit,
        int remaining,
        long resetAfterMs
) {

    /** Convenience — seconds until reset, used for Retry-After header. */
    public long retryAfterSeconds() {
        return (resetAfterMs / 1000) + 1; // +1 for safety margin
    }
}
