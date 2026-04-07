package com.email.writer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimiterService unit tests")
class RateLimiterServiceTest {

    private RateLimiterService rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiterService();
    }

    @Test
    @DisplayName("canMakeRequest — first request is always allowed")
    void canMakeRequest_firstRequest_allowed() {
        assertThat(rateLimiter.canMakeRequest("user1")).isTrue();
    }

    @Test
    @DisplayName("canMakeRequest — different users are tracked independently")
    void canMakeRequest_differentUsers_independent() {
        // Exhaust user1's minute limit
        for (int i = 0; i < 8; i++) {
            rateLimiter.recordRequest("user1");
        }

        // user1 should be rate-limited
        assertThat(rateLimiter.canMakeRequest("user1")).isFalse();
        // user2 should still be allowed
        assertThat(rateLimiter.canMakeRequest("user2")).isTrue();
    }

    @Test
    @DisplayName("canMakeRequest — blocked after 8 requests in one minute")
    void canMakeRequest_blockedAfter8RequestsPerMinute() {
        String user = "testuser";
        for (int i = 0; i < 8; i++) {
            assertThat(rateLimiter.canMakeRequest(user)).isTrue();
            rateLimiter.recordRequest(user);
        }
        // 9th request should be blocked
        assertThat(rateLimiter.canMakeRequest(user)).isFalse();
    }

    @Test
    @DisplayName("remainingMinute — decreases with each recorded request")
    void remainingMinute_decreasesWithEachRequest() {
        String user = "testuser";
        assertThat(rateLimiter.remainingMinute(user)).isEqualTo(8);

        rateLimiter.recordRequest(user);
        assertThat(rateLimiter.remainingMinute(user)).isEqualTo(7);

        rateLimiter.recordRequest(user);
        assertThat(rateLimiter.remainingMinute(user)).isEqualTo(6);
    }

    @Test
    @DisplayName("remainingMinute — returns 0 when fully rate-limited")
    void remainingMinute_zeroWhenRateLimited() {
        String user = "testuser";
        for (int i = 0; i < 8; i++) {
            rateLimiter.recordRequest(user);
        }
        assertThat(rateLimiter.remainingMinute(user)).isEqualTo(0);
    }
}