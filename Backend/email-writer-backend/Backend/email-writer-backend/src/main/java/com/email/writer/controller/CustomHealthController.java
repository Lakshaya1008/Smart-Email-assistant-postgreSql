package com.email.writer.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class CustomHealthController {
    private final HealthEndpoint healthEndpoint;

    public CustomHealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/custom-health")
    public ResponseEntity<Map<String, Object>> health() {
        HealthComponent healthComponent = healthEndpoint.health();
        String status = (healthComponent instanceof Health h) ? h.getStatus().getCode() : "UNKNOWN";
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);

        Map<String, String> checks = new LinkedHashMap<>();
        if (healthComponent instanceof Health h && h.getDetails() != null) {
            for (Map.Entry<String, Object> entry : h.getDetails().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof HealthComponent hc) {
                    if (hc instanceof Health subHealth) {
                        checks.put(entry.getKey(), subHealth.getStatus().getCode());
                    }
                } else if (value instanceof Map<?, ?> map) {
                    Object st = map.get("status");
                    if (st != null) {
                        checks.put(entry.getKey(), st.toString());
                    }
                }
            }
        }
        response.put("checks", checks);

        // If any check is DOWN, set status to DEGRADED and HTTP 503
        boolean anyDown = checks.values().stream().anyMatch(v -> v.equalsIgnoreCase("DOWN"));
        if (anyDown) {
            response.put("status", "DEGRADED");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
