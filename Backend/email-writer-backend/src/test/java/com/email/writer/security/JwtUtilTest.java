package com.email.writer.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil unit tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 64-char secret — matches minimum required for HS512
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-that-is-long-enough-for-hs512-algorithm-minimum-64-chars!!");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 86_400_000L); // 24h

        testUser = new User("testuser", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("generateToken — token is not null or blank")
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateToken — token has three JWT parts")
    void generateToken_hasThreeParts() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractUsername — returns correct username from token")
    void extractUsername_returnsCorrectUsername() {
        String token = jwtUtil.generateToken(testUser);
        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("validateToken — valid token and matching user returns true")
    void validateToken_validTokenMatchingUser_returnsTrue() {
        String token = jwtUtil.generateToken(testUser);
        boolean valid = jwtUtil.validateToken(token, testUser);
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("validateToken — token for different user returns false")
    void validateToken_differentUser_returnsFalse() {
        String token = jwtUtil.generateToken(testUser);
        UserDetails otherUser = new User("otheruser", "password", Collections.emptyList());
        boolean valid = jwtUtil.validateToken(token, otherUser);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("extractExpiration — expiry is in the future")
    void extractExpiration_isInFuture() {
        String token = jwtUtil.generateToken(testUser);
        Date expiry = jwtUtil.extractExpiration(token);
        assertThat(expiry).isAfter(new Date());
    }

    @Test
    @DisplayName("validateToken — expired token throws exception")
    void validateToken_expiredToken_throwsException() {
        // Set expiration to 1ms so token expires immediately
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 1L);
        String token = jwtUtil.generateToken(testUser);

        // Wait for expiry
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThatThrownBy(() -> jwtUtil.validateToken(token, testUser))
                .isInstanceOf(Exception.class); // ExpiredJwtException
    }
}