package com.saas.libms.config;

import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {
//Tier 1: Global limit
    // Protects against general DDoS / traffic floods.
    // Applied to every request, regardless of endpoint.

    /** Max requests allowed per IP in the global window. */
    public static final int GLOBAL_LIMIT = 200;

    /** The rolling time window for the global limit. */
    public static final Duration GLOBAL_WINDOW = Duration.ofMinutes(1);

    //Tier 2: Endpoint-specific limits
    // Applied on top of the global limit for sensitive endpoints.

    /** POST /api/v1/auth/login — credential stuffing protection. */
    public static final int    LOGIN_LIMIT  = 10;
    public static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    /** POST /api/v1/auth/register — spam account / abuse protection. */
    public static final int    REGISTER_LIMIT  = 3;
    public static final Duration REGISTER_WINDOW = Duration.ofHours(1);

    /** POST /api/v1/auth/verify — brute-force verification code protection. */
    public static final int    VERIFY_LIMIT  = 5;
    public static final Duration VERIFY_WINDOW = Duration.ofMinutes(15);

    /** POST /api/v1/auth/forgot-password — email flooding protection. */
    public static final int    FORGOT_PASSWORD_LIMIT  = 3;
    public static final Duration FORGOT_PASSWORD_WINDOW = Duration.ofMinutes(15);

    //Redis key prefixes
    // Each rate limit bucket is stored as a Redis sorted set under these prefixes.
    // Full key pattern: "rl:{PREFIX}:{clientKey}"
    // Example:          "rl:GLOBAL:192.168.1.1"
    //                   "rl:LOGIN:192.168.1.1|Mozilla/5.0|en-US"

    public static final String PREFIX_GLOBAL          = "GLOBAL";
    public static final String PREFIX_LOGIN           = "LOGIN";
    public static final String PREFIX_REGISTER        = "REGISTER";
    public static final String PREFIX_VERIFY          = "VERIFY";
    public static final String PREFIX_FORGOT_PASSWORD = "FORGOT_PWD";

}
