package com.saas.libms.ratelimit;

import com.saas.libms.config.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "rl:";

    public RateLimitResult check(String prefix, String clientKey, int limit, Duration window) {
        String key = KEY_PREFIX + prefix + ":" + clientKey;
        long nowMs = Instant.now().toEpochMilli();
        long windowMs = window.toMillis();
        long windowStartMs = nowMs - windowMs;

        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();

        // Step 1,Remove timestamps that have fallen outside the window
        zset.removeRangeByScore(key, 0, windowStartMs);

        // Step 2,Count how many requests are in the current window
        Long count = zset.zCard(key);
        int currentCount = (count == null) ? 0 : count.intValue();

        // Step 3,Check if limit is exceeded
        if (currentCount >= limit) {
            // Find the oldest entry in the window the client must wait until
            // it falls out of the window before their count drops below the limit
            long resetAfterMs = computeResetAfterMs(zset, key, windowMs, nowMs);
            int remaining = 0;
            log.debug("Rate limit BLOCKED — key={}, count={}/{}", key, currentCount, limit);
            return new RateLimitResult(false, limit, remaining, resetAfterMs);
        }

        // Step 4,Allow: record this request timestamp
        // Use nowMs as both member and score. If two requests arrive in the same
        // millisecond, append a small unique suffix to avoid member collision.
        String member = nowMs + "-" + Thread.currentThread().getId();
        zset.add(key, member, nowMs);

        // Step 5,Refresh TTL so the key expires naturally when the window ends
        // (avoids orphaned keys in Redis if no more requests come)
        redisTemplate.expire(key, windowMs + 1000, TimeUnit.MILLISECONDS);

        int remaining = limit - currentCount - 1;
        log.debug("Rate limit ALLOWED — key={}, count={}/{}, remaining={}", key, currentCount + 1, limit, remaining);
        return new RateLimitResult(true, limit, remaining, windowMs);
    }

    /**
     * Calculate how long (in ms) until the oldest entry in the window expires.
     * This tells the client how long to wait before retrying.
     */
    private long computeResetAfterMs(ZSetOperations<String, String> zset,
                                     String key, long windowMs, long nowMs) {
        // Get the oldest entry (lowest score = oldest timestamp)
        var oldest = zset.rangeByScoreWithScores(key, 0, nowMs,
                0, 1); // offset=0, count=1

        if (oldest == null || oldest.isEmpty()) {
            return windowMs; // fallback - full window
        }

        double oldestScoreMs = oldest.iterator().next().getScore();
        // The oldest entry will fall out of the window at: oldestScore + windowMs
        long expiresAtMs = (long) oldestScoreMs + windowMs;
        return Math.max(0, expiresAtMs - nowMs);
    }
}
