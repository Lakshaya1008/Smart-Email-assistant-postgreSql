package com.email.writer.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler — centralises error responses.
 *
 * Security rule: raw exception messages (which can contain table names,
 * query details, or stack traces) are NEVER sent to the client.
 * They are logged server-side only. Clients receive a safe generic message.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Bean validation errors (@Valid on DTOs) — safe to return field details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());

        List<Map<String, String>> fieldErrors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            Map<String, String> err = new HashMap<>();
            err.put("field", fieldError.getField());
            err.put("message", fieldError.getDefaultMessage());
            fieldErrors.add(err);
        });

        Map<String, Object> body = new HashMap<>();
        body.put("error", "validation_error");
        body.put("message", "Validation failed for one or more fields.");
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Known business errors thrown by service/auth layer.
     * These carry user-safe messages (e.g. "Username already exists",
     * "Invalid username or password", "Reply not found with ID: 5").
     *
     * We allow the message through only for known safe prefixes.
     * Everything else gets a generic 500.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";

        // These messages come from our own code and are safe to surface
        boolean isSafeUserMessage =
                msg.contains("Username already exists") ||
                        msg.contains("Email already exists") ||
                        msg.contains("Invalid username or password") ||
                        msg.contains("User not authenticated") ||
                        msg.contains("User not found") ||
                        msg.contains("Reply not found") ||
                        msg.contains("Access denied:") ||
                        msg.startsWith("Username") ||
                        msg.startsWith("Email");

        if (isSafeUserMessage) {
            // Determine correct status
            HttpStatus status = HttpStatus.BAD_REQUEST;
            if (msg.contains("not found")) status = HttpStatus.NOT_FOUND;
            if (msg.contains("Access denied") || msg.contains("not authenticated")) {
                status = HttpStatus.UNAUTHORIZED;
            }

            log.warn("Business rule violation [{}]: {}", status, msg);
            return ResponseEntity.status(status).body(Map.of(
                    "error", "request_error",
                    "message", msg
            ));
        }

        // Unknown RuntimeException — could be DB error, NPE, Gemini failure, etc.
        // Log the full details server-side; return a generic message to client.
        log.error("Unhandled RuntimeException: {}", msg, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "internal_error",
                "message", "An unexpected error occurred. Please try again."
        ));
    }

    /**
     * Catch-all for any other exception types.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        log.error("Unhandled exception [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "internal_error",
                "message", "An unexpected error occurred. Please try again."
        ));
    }
}