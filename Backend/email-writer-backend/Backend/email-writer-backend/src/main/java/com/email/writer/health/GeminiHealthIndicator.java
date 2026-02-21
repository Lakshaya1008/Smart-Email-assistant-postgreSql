package com.email.writer.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Component
public class GeminiHealthIndicator implements HealthIndicator {
    private final RestTemplate restTemplate;
    @Value("${gemini.api.url}")
    private String geminiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public GeminiHealthIndicator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + geminiApiKey);
            RequestEntity<Void> request = RequestEntity.head(geminiUrl).headers(headers).build();
            ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return Health.up().build();
            } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return Health.down().withDetail("error", "Gemini API key invalid").build();
            } else {
                return Health.status("DEGRADED").withDetail("error", "Gemini returned " + response.getStatusCode()).build();
            }
        } catch (Exception e) {
            return Health.status("DEGRADED").withDetail("error", e.getMessage()).build();
        }
    }
}
