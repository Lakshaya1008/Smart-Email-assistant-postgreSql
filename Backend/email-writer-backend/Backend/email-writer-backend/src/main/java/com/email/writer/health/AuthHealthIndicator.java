package com.email.writer.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import com.email.writer.security.JwtUtil;

@Component
public class AuthHealthIndicator implements HealthIndicator {
    private final JwtUtil jwtUtil;

    public AuthHealthIndicator(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Health health() {
        try {
            // Attempt to parse a dummy token format
            jwtUtil.validateTokenFormat("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkdW1teSIsImlhdCI6MTYwOTk5OTk5OSwiZXhwIjoxNjA5OTk5OTk5fQ.abc123");
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
