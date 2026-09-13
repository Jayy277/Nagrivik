package org.nagrivic.modules.moderation.ratelimit;

import java.time.Duration;

/**
 * Abstraction for rate limiting high-risk public or citizen endpoints.
 * Allows pluggable replacement with a distributed store (such as Redis) in production.
 */
public interface RateLimiter {

    record RateLimitResult(boolean allowed, long retryAfterSeconds, int remainingRequests) {
        public static RateLimitResult permitted(int remaining) {
            return new RateLimitResult(true, 0L, remaining);
        }

        public static RateLimitResult rejected(long retryAfterSeconds) {
            return new RateLimitResult(false, Math.max(1L, retryAfterSeconds), 0);
        }
    }

    /**
     * Attempts to acquire a permit under the given key, limit, and time window.
     *
     * @param key unique identifier (e.g. userId:action or ip:endpoint)
     * @param maxRequests maximum allowed requests within window
     * @param window duration of the sliding time window
     * @return result indicating if request is permitted or retry interval if rejected
     */
    RateLimitResult tryAcquire(String key, int maxRequests, Duration window);

    /**
     * Helper that acquires or throws RateLimitExceededException if exceeded.
     */
    default void checkLimit(String key, int maxRequests, Duration window) {
        RateLimitResult result = tryAcquire(key, maxRequests, window);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
    }
}
