package com.email.writer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user server-side rate limiter using a sliding window algorithm.
 *
 * Why this exists: The frontend had a localStorage-only rate limiter that was
 * trivially bypassed — opening an incognito tab or a different browser reset
 * all limits. The shared Gemini API key could be exhausted by any single user.
 *
 * This service enforces limits on the server where they cannot be bypassed:
 * - 8 requests per minute per user (matches Gemini free tier)
 * - 200 requests per day per user
 *
 * Implementation: ConcurrentHashMap of username → deque of timestamps.
 * The deque is trimmed on every check so expired entries don't accumulate.
 * State is in-memory (single instance) — sufficient for free-tier Render
 * deployment. For multi-instance deployments, replace with Redis.
 */
@Service
@Slf4j
public class RateLimiterService {

    private static final int  MAX_PER_MINUTE = 8;
    private static final int  MAX_PER_DAY    = 200;
    private static final long ONE_MINUTE_MS  = 60_000L;
    private static final long ONE_DAY_MS     = 86_400_000L;

    // Separate maps for minute-window and day-window tracking
    private final ConcurrentHashMap<String, Deque<Long>> minuteWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Long>> dayWindows    = new ConcurrentHashMap<>();

    /**
     * Check if the user can make a request. Does NOT record the request.
     * Call recordRequest() separately only if you proceed.
     *
     * @param username the authenticated username
     * @return true if within limits, false if rate-limited
     */
    public boolean canMakeRequest(String username) {
        long now = System.currentTimeMillis();
        return withinLimit(minuteWindows, username, now, ONE_MINUTE_MS, MAX_PER_MINUTE)
                && withinLimit(dayWindows,    username, now, ONE_DAY_MS,    MAX_PER_DAY);
    }

    /**
     * Record that the user has made a request. Call after canMakeRequest() returns true.
     */
    public void recordRequest(String username) {
        long now = System.currentTimeMillis();
        record(minuteWindows, username, now, ONE_MINUTE_MS);
        record(dayWindows,    username, now, ONE_DAY_MS);
        log.debug("Rate limit recorded for user {} — minute: {}, day: {}",
                username,
                minuteWindows.getOrDefault(username, new ArrayDeque<>()).size(),
                dayWindows.getOrDefault(username, new ArrayDeque<>()).size()
        );
    }

    /**
     * Return remaining requests for the user (for response headers / debugging).
     */
    public int remainingMinute(String username) {
        long now = System.currentTimeMillis();
        Deque<Long> window = minuteWindows.getOrDefault(username, new ArrayDeque<>());
        synchronized (window) {
            trimExpired(window, now, ONE_MINUTE_MS);
            return Math.max(0, MAX_PER_MINUTE - window.size());
        }
    }

    /* ── internal helpers ─────────────────────────────────────────────── */

    private boolean withinLimit(ConcurrentHashMap<String, Deque<Long>> map,
                                String username, long now, long windowMs, int limit) {
        Deque<Long> window = map.computeIfAbsent(username, k -> new ArrayDeque<>());
        synchronized (window) {
            trimExpired(window, now, windowMs);
            return window.size() < limit;
        }
    }

    private void record(ConcurrentHashMap<String, Deque<Long>> map,
                        String username, long now, long windowMs) {
        Deque<Long> window = map.computeIfAbsent(username, k -> new ArrayDeque<>());
        synchronized (window) {
            trimExpired(window, now, windowMs);
            window.addLast(now);
        }
    }

    private void trimExpired(Deque<Long> window, long now, long windowMs) {
        while (!window.isEmpty() && (now - window.peekFirst()) > windowMs) {
            window.pollFirst();
        }
    }
}