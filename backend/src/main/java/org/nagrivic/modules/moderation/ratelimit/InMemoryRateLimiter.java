package org.nagrivic.modules.moderation.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory sliding-window rate limiter.
 *
 * NOTE & LIMITATION:
 * This in-memory implementation operates on a single JVM node. For multi-instance, clustered
 * production deployments, replace this with a distributed implementation (e.g., Redis-backed
 * sliding window) without changing consumers of RateLimiter.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();

    @Override
    public synchronized RateLimitResult tryAcquire(String key, int maxRequests, Duration window) {
        long now = Instant.now().toEpochMilli();
        long windowMillis = window.toMillis();
        long cutoff = now - windowMillis;

        Deque<Long> timestamps = requestWindows.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Evict timestamps outside current sliding window
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            Long oldest = timestamps.peekFirst();
            long retryAfterMillis = (oldest != null) ? (oldest + windowMillis - now) : windowMillis;
            long retryAfterSeconds = (retryAfterMillis + 999) / 1000;
            return RateLimitResult.rejected(retryAfterSeconds);
        }

        timestamps.addLast(now);
        return RateLimitResult.permitted(maxRequests - timestamps.size());
    }

    /**
     * Clears all recorded rate limit windows (useful for isolated unit tests).
     */
    public void reset() {
        requestWindows.clear();
    }
}
