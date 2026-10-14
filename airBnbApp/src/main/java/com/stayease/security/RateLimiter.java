package com.stayease.security;

import com.stayease.exception.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory fixed-window rate limiter.
 * <p>
 * State is per application instance; if the backend is ever scaled horizontally,
 * move this to a shared store (e.g. Redis) so limits apply across instances.
 */
@Component
public class RateLimiter {

    private record Window(Instant start, Duration length, int count) {
        boolean isExpired(Instant now) {
            return !now.isBefore(start.plus(length));
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Records one attempt for {@code key}.
     *
     * @throws TooManyRequestsException if more than {@code limit} attempts were made in the current window
     */
    public void consume(String key, int limit, Duration windowLength) {
        Instant now = Instant.now();
        Window window = windows.compute(key, (k, current) ->
                current == null || current.isExpired(now)
                        ? new Window(now, windowLength, 1)
                        : new Window(current.start(), current.length(), current.count() + 1));

        if (window.count() > limit) {
            long retryAfter = Math.max(1, Duration.between(now, window.start().plus(window.length())).toSeconds());
            throw new TooManyRequestsException("Too many attempts, please try again later", retryAfter);
        }
    }

    @Scheduled(fixedRate = 60_000)
    void evictExpiredWindows() {
        Instant now = Instant.now();
        windows.values().removeIf(window -> window.isExpired(now));
    }
}
