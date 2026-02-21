package com.email.writer.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Component
public class GeminiHealthIndicator implements HealthIndicator {
    private final RestTemplate restTemplate;
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.endpoint}")
    private String geminiApiEndpoint;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public GeminiHealthIndicator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            // Use the real API call format: endpoint + ?key=API_KEY
            String url = geminiApiUrl + geminiApiEndpoint + "?key=" + geminiApiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Minimal valid payload for Gemini
            String payload = "{\"contents\":[{\"parts\":[{\"text\":\"ping\"}]}]}";
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
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
