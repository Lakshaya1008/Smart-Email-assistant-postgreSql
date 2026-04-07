package com.email.writer.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom health endpoint that aggregates actuator health details into a
 * clean JSON response suitable for monitoring dashboards and cron job pings.
 *
 * This controller was previously only in the inner (undeployed) copy of the
 * backend. Brought to the outer (deployed) copy so that:
 *   1. /custom-health actually exists in production
 *   2. Cron job keepalive pings work correctly
 *   3. The response format is human-readable (status + per-service breakdown)
 *
 * Endpoint: GET /custom-health (public — whitelisted in SecurityConfig)
 *
 * Response (healthy):
 *   { "status": "UP", "services": { "database": "UP" } }
 *
 * Response (degraded):
 *   { "status": "DOWN", "services": { "database": "DOWN" } }
 *   HTTP 503 Service Unavailable
 *
 * For cron job pings: use /actuator/health (simpler) or /custom-health (richer).
 * Do NOT use /api/email/test — it makes a real Gemini API call and wastes quota.
 */
@RestController
public class CustomHealthController {

    private final HealthEndpoint healthEndpoint;

    public CustomHealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/custom-health")
    public ResponseEntity<Map<String, Object>> health() {
        Health health = (Health) healthEndpoint.health();
        Map<String, String> services = new LinkedHashMap<>();
        String overallStatus = "UP";
        boolean anyDown = false;

        if (health.getDetails() != null) {
            for (Map.Entry<String, Object> entry : health.getDetails().entrySet()) {
                String key = entry.getKey();
                // Map actuator internal names to readable keys
                String mappedKey = key.equalsIgnoreCase("db") ? "database" : key;
                String status = "UNKNOWN";
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> map) {
                    Object st = map.get("status");
                    if (st != null) status = st.toString();
                }
                services.put(mappedKey, status);
                if (status.equalsIgnoreCase("DOWN")) anyDown = true;
            }
        }

        if (anyDown) overallStatus = "DOWN";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", overallStatus);
        response.put("services", services);

        HttpStatus httpStatus = overallStatus.equals("UP") ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(response);
    }
}