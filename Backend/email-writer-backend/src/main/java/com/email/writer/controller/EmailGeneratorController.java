package com.email.writer.controller;

import com.email.writer.dto.EmailRequest;
import com.email.writer.service.AuthService;
import com.email.writer.service.EmailGeneratorService;
import com.email.writer.service.RateLimiterService;
import com.email.writer.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Email generation endpoints — versioned at /api/v1/email.
 * Server-side rate limiting added via RateLimiterService.
 */
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Slf4j
public class EmailGeneratorController {

    private final EmailGeneratorService emailGeneratorService;
    private final AuthService           authService;
    private final RateLimiterService    rateLimiterService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateEmail(
            @Valid @RequestBody EmailRequest request,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication);

        // Server-side rate limit — cannot be bypassed by the client
        if (!rateLimiterService.canMakeRequest(user.getUsername())) {
            log.warn("Rate limit exceeded for user {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error",   "rate_limit_exceeded",
                            "message", "You have reached the request limit (8 per minute / 200 per day). Please wait before trying again."
                    ));
        }

        rateLimiterService.recordRequest(user.getUsername());
        log.info("Email generate for user {} (subject={})", user.getUsername(), request.getSubject());

        Map<String, Object> result = emailGeneratorService.generateMultipleEmailReplies(request, false);

        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimiterService.remainingMinute(user.getUsername())))
                .body(result);
    }

    @PostMapping("/regenerate")
    public ResponseEntity<?> regenerateEmail(
            @Valid @RequestBody EmailRequest request,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication);

        if (!rateLimiterService.canMakeRequest(user.getUsername())) {
            log.warn("Rate limit exceeded for user {} on regenerate", user.getUsername());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error",   "rate_limit_exceeded",
                            "message", "You have reached the request limit. Please wait before trying again."
                    ));
        }

        rateLimiterService.recordRequest(user.getUsername());
        log.info("Email regenerate for user {} (subject={})", user.getUsername(), request.getSubject());
        Map<String, Object> result = emailGeneratorService.generateMultipleEmailReplies(request, true);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate-single")
    public ResponseEntity<?> generateSingle(
            @Valid @RequestBody EmailRequest request,
            Authentication authentication) {

        User user = authService.getCurrentUser(authentication);

        if (!rateLimiterService.canMakeRequest(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "rate_limit_exceeded", "message", "Request limit reached. Please wait."));
        }

        rateLimiterService.recordRequest(user.getUsername());
        Map<String, String> response = emailGeneratorService.generateEmailReply(request);
        return ResponseEntity.ok(Map.of(
                "summary", response.getOrDefault("summary", ""),
                "reply",   response.getOrDefault("reply",   "")
        ));
    }

    /**
     * Lightweight ping — NO Gemini call, safe for cron job keepalive.
     * Use this URL in cron-job.org / UptimeRobot, not /api/v1/email/test
     * which would burn Gemini quota on every ping.
     */
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "email-generator"));
    }

    /** Connectivity test — makes a real Gemini call. Use sparingly. */
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        EmailRequest testReq = new EmailRequest();
        testReq.setSubject("Test");
        testReq.setEmailContent("Testing connectivity");
        testReq.setTone("professional");
        testReq.setLanguage("en");
        Map<String, String> response = emailGeneratorService.generateEmailReply(testReq);
        return ResponseEntity.ok(Map.of(
                "status",  "ok",
                "summary", response.getOrDefault("summary", ""),
                "reply",   response.getOrDefault("reply",   "")
        ));
    }
}