package com.saas.libms.ratelimit;

import com.saas.libms.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * Unit tests for RateLimitService.
 *
 * These tests use Mockito to mock Redis — no real Redis needed.
 * They verify the sliding window logic: counting, blocking, and
 * result fields (allowed, remaining, resetAfterMs).
 *
 * To run: mvn test -Dtest=RateLimitServiceTest
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    private StringRedisTemplate redisTemplate;
    private ZSetOperations<String, String> zsetOps;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        zsetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zsetOps);
        rateLimitService = new RateLimitService(redisTemplate);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Simulate Redis returning a specific current count for the sliding window. */
    private void givenCurrentCount(long count) {
        when(zsetOps.zCard(anyString())).thenReturn(count);
    }

    /** Simulate Redis returning no oldest entry (empty window). */
    private void givenEmptyWindow() {
        when(zsetOps.rangeByScoreWithScores(anyString(), anyDouble(), anyDouble(),
                anyLong(), anyLong())).thenReturn(new LinkedHashSet<>());
    }

    /** Simulate Redis returning an oldest entry with the given timestamp score. */
    private void givenOldestEntryAt(double timestampMs) {
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getScore()).thenReturn(timestampMs);
        Set<ZSetOperations.TypedTuple<String>> result = new LinkedHashSet<>();
        result.add(tuple);
        when(zsetOps.rangeByScoreWithScores(anyString(), anyDouble(), anyDouble(),
                anyLong(), anyLong())).thenReturn(result);
    }

    // ── Tests: Allowed ────────────────────────────────────────────────────────

    @Test
    @DisplayName("First request on an empty key should be allowed")
    void firstRequest_shouldBeAllowed() {
        givenCurrentCount(0L); // no previous requests

        RateLimitResult result = rateLimitService.check(
                "TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.remaining()).isEqualTo(9); // 10 - 0 - 1
    }

    @Test
    @DisplayName("Request exactly at the limit boundary should be allowed")
    void requestAtBoundary_shouldBeAllowed() {
        givenCurrentCount(9L); // 9 requests already in window, limit is 10

        RateLimitResult result = rateLimitService.check(
                "TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isEqualTo(0); // this was the last allowed one
    }

    @Test
    @DisplayName("Remaining count should decrease correctly as requests accumulate")
    void remainingCount_shouldDecreaseCorrectly() {
        givenCurrentCount(5L); // 5 requests in window, limit is 10

        RateLimitResult result = rateLimitService.check(
                "TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isEqualTo(4); // 10 - 5 - 1
    }

    @Test
    @DisplayName("Allowed result should have correct limit value")
    void allowedResult_shouldHaveCorrectLimit() {
        givenCurrentCount(0L);

        RateLimitResult result = rateLimitService.check(
                RateLimitConfig.PREFIX_LOGIN, "192.168.1.1",
                RateLimitConfig.LOGIN_LIMIT, RateLimitConfig.LOGIN_WINDOW);

        assertThat(result.limit()).isEqualTo(RateLimitConfig.LOGIN_LIMIT);
        assertThat(result.allowed()).isTrue();
    }

    // ── Tests: Blocked ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Request exceeding limit should be blocked")
    void requestOverLimit_shouldBeBlocked() {
        givenCurrentCount(10L);   // already at limit
        givenEmptyWindow();       // no oldest entry to compute reset from

        RateLimitResult result = rateLimitService.check(
                "TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isEqualTo(0);
    }

    @Test
    @DisplayName("Blocked result should have remaining = 0")
    void blockedResult_remainingShouldBeZero() {
        givenCurrentCount(100L);
        givenEmptyWindow();

        RateLimitResult result = rateLimitService.check(
                "TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        assertThat(result.remaining()).isEqualTo(0);
    }

    @Test
    @DisplayName("Blocked result should NOT call zset.add (no new entry recorded)")
    void blockedRequest_shouldNotAddToZSet() {
        givenCurrentCount(10L);
        givenEmptyWindow();

        rateLimitService.check("TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        // zset.add should never be called when the request is blocked
        verify(zsetOps, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("Allowed request should call zset.add exactly once")
    void allowedRequest_shouldAddToZSet() {
        givenCurrentCount(0L);

        rateLimitService.check("TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        verify(zsetOps, times(1)).add(anyString(), anyString(), anyDouble());
    }

    // ── Tests: Reset / Retry-After ────────────────────────────────────────────

    @Test
    @DisplayName("Blocked result resetAfterMs should be positive")
    void blockedResult_resetAfterMs_shouldBePositive() {
        givenCurrentCount(10L);
        // Oldest entry was 30 seconds ago; with a 60-second window it expires in 30 seconds
        long thirtySecondsAgoMs = System.currentTimeMillis() - 30_000;
        givenOldestEntryAt(thirtySecondsAgoMs);

        RateLimitResult result = rateLimitService.check(
                "TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        assertThat(result.resetAfterMs()).isGreaterThan(0);
        // Should be approximately 30 seconds (allow ±1 second for test execution time)
        assertThat(result.resetAfterMs()).isBetween(28_000L, 32_000L);
    }

    @Test
    @DisplayName("retryAfterSeconds() should be at least 1")
    void retryAfterSeconds_shouldBeAtLeastOne() {
        givenCurrentCount(10L);
        givenEmptyWindow();

        RateLimitResult result = rateLimitService.check(
                "TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        assertThat(result.retryAfterSeconds()).isGreaterThanOrEqualTo(1);
    }

    // ── Tests: Redis key format ───────────────────────────────────────────────

    @Test
    @DisplayName("Redis key should follow the pattern rl:{prefix}:{clientKey}")
    void redisKey_shouldFollowExpectedPattern() {
        givenCurrentCount(0L);

        rateLimitService.check("LOGIN", "192.168.1.1", 10, Duration.ofMinutes(1));

        // Verify removeRangeByScore was called with a key matching our pattern
        verify(zsetOps).removeRangeByScore(
                eq("rl:LOGIN:192.168.1.1"), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Different prefixes should use different Redis keys")
    void differentPrefixes_shouldUseDifferentKeys() {
        givenCurrentCount(0L);

        rateLimitService.check("LOGIN", "192.168.1.1", 10, Duration.ofMinutes(1));
        rateLimitService.check("GLOBAL", "192.168.1.1", 200, Duration.ofMinutes(1));

        verify(zsetOps).removeRangeByScore(
                eq("rl:LOGIN:192.168.1.1"), anyDouble(), anyDouble());
        verify(zsetOps).removeRangeByScore(
                eq("rl:GLOBAL:192.168.1.1"), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Different client keys should use different Redis keys")
    void differentClientKeys_shouldBeSeparate() {
        givenCurrentCount(0L);

        rateLimitService.check("GLOBAL", "10.0.0.1", 200, Duration.ofMinutes(1));
        rateLimitService.check("GLOBAL", "10.0.0.2", 200, Duration.ofMinutes(1));

        verify(zsetOps).removeRangeByScore(
                eq("rl:GLOBAL:10.0.0.1"), anyDouble(), anyDouble());
        verify(zsetOps).removeRangeByScore(
                eq("rl:GLOBAL:10.0.0.2"), anyDouble(), anyDouble());
    }

    // ── Tests: TTL management ─────────────────────────────────────────────────

    @Test
    @DisplayName("Allowed request should set TTL on the Redis key")
    void allowedRequest_shouldSetTTL() {
        givenCurrentCount(0L);

        rateLimitService.check("TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        verify(redisTemplate).expire(anyString(), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Blocked request should NOT reset TTL (key expiry should not be disturbed)")
    void blockedRequest_shouldNotSetTTL() {
        givenCurrentCount(10L);
        givenEmptyWindow();

        rateLimitService.check("TEST", "192.168.1.1", 10, Duration.ofMinutes(1));

        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    // ── Tests: Real-world limit configs ──────────────────────────────────────

    @Test
    @DisplayName("Login limit: 10 requests per 15 minutes — 11th should be blocked")
    void loginLimit_eleventhRequest_shouldBeBlocked() {
        givenCurrentCount(10L);  // 10 already in window
        givenEmptyWindow();

        RateLimitResult result = rateLimitService.check(
                RateLimitConfig.PREFIX_LOGIN, "192.168.1.1",
                RateLimitConfig.LOGIN_LIMIT, RateLimitConfig.LOGIN_WINDOW);

        assertThat(result.allowed()).isFalse();
    }

    @Test
    @DisplayName("Register limit: 3 per hour — 4th should be blocked")
    void registerLimit_fourthRequest_shouldBeBlocked() {
        givenCurrentCount(3L);
        givenEmptyWindow();

        RateLimitResult result = rateLimitService.check(
                RateLimitConfig.PREFIX_REGISTER, "192.168.1.1",
                RateLimitConfig.REGISTER_LIMIT, RateLimitConfig.REGISTER_WINDOW);

        assertThat(result.allowed()).isFalse();
    }

    @Test
    @DisplayName("Forgot password limit: 3 per 15 min — 4th should be blocked")
    void forgotPasswordLimit_fourthRequest_shouldBeBlocked() {
        givenCurrentCount(3L);
        givenEmptyWindow();

        RateLimitResult result = rateLimitService.check(
                RateLimitConfig.PREFIX_FORGOT_PASSWORD, "192.168.1.1",
                RateLimitConfig.FORGOT_PASSWORD_LIMIT, RateLimitConfig.FORGOT_PASSWORD_WINDOW);

        assertThat(result.allowed()).isFalse();
    }

    @Test
    @DisplayName("Global limit: 200 per minute — 201st should be blocked")
    void globalLimit_201stRequest_shouldBeBlocked() {
        givenCurrentCount(200L);
        givenEmptyWindow();

        RateLimitResult result = rateLimitService.check(
                RateLimitConfig.PREFIX_GLOBAL, "192.168.1.1",
                RateLimitConfig.GLOBAL_LIMIT, RateLimitConfig.GLOBAL_WINDOW);

        assertThat(result.allowed()).isFalse();
    }
}