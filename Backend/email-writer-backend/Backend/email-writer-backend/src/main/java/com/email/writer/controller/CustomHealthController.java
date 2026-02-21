package com.email.writer.controller;

import org.springframework.boot.actuate.health.Health;
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
        Health health = (Health) healthEndpoint.health();
        Map<String, String> services = new LinkedHashMap<>();
        String overallStatus = "UP";
        boolean anyDegraded = false;
        boolean anyDown = false;

        // Map actuator component names to your desired keys
        if (health.getDetails() != null) {
            for (Map.Entry<String, Object> entry : health.getDetails().entrySet()) {
                String key = entry.getKey();
                String mappedKey = key;
                if (key.equalsIgnoreCase("db")) mappedKey = "database";
                if (key.equalsIgnoreCase("geminiHealthIndicator")) mappedKey = "gemini";
                if (key.equalsIgnoreCase("authHealthIndicator")) mappedKey = "auth";
                String status = "UNKNOWN";
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> map) {
                    Object st = map.get("status");
                    if (st != null) status = st.toString();
                }
                services.put(mappedKey, status);
                if (status.equalsIgnoreCase("DEGRADED")) anyDegraded = true;
                if (status.equalsIgnoreCase("DOWN")) anyDown = true;
            }
        }
        if (anyDown) overallStatus = "DOWN";
        else if (anyDegraded) overallStatus = "DEGRADED";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", overallStatus);
        response.put("services", services);

        HttpStatus httpStatus = overallStatus.equals("UP") ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(response);
    }
}
