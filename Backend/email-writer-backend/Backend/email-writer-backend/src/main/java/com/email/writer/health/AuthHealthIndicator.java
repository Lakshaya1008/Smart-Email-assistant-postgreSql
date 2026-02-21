package com.email.writer.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import com.email.writer.security.JwtUtil;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@Component
public class AuthHealthIndicator implements HealthIndicator {
    private final JwtUtil jwtUtil;

    public AuthHealthIndicator(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Health health() {
        try {
            // Generate a valid token using the secret from env
            UserDetails dummyUser = User.withUsername("dummyuser").password("dummy").roles("USER").build();
            String token = jwtUtil.generateToken(dummyUser);
            jwtUtil.validateTokenFormat(token);
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
