package com.email.writer.service;

import com.email.writer.dto.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

/**
 * Calls the Gemini API to generate email replies.
 *
 * Phase 8 change: Replaced WebClient (reactive) + .block() with RestTemplate.
 * The previous code imported spring-boot-starter-webflux (reactive framework)
 * and then immediately called .block() to convert back to blocking — adding
 * the full overhead of Project Reactor with zero benefit. RestTemplate is
 * honest about being synchronous, simpler, and has identical behaviour.
 * spring-boot-starter-webflux has been removed from pom.xml.
 */
@Service
@Slf4j
public class EmailGeneratorService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.endpoint}")
    private String geminiApiEndpoint;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // RestTemplate is now injected — configured as a bean in AppConfig
    // with a 30-second read timeout so Gemini calls cannot hang forever.
    public EmailGeneratorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Generate three reply variations plus a short summary. */
    public Map<String, Object> generateMultipleEmailReplies(EmailRequest request, boolean regenerate) {
        final String language = resolveLanguage(request.getLanguage());
        String prompt = buildMultipleRepliesPrompt(request, language, regenerate);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature",     regenerate ? 0.9 : 0.75,
                        "maxOutputTokens", 2048,
                        "topP",            0.95,
                        "topK",            40
                )
        );

        String raw = callGemini(requestBody);
        return parseMultipleRepliesResponse(raw);
    }

    /** Generate a single email reply with summary (backwards-compat). */
    public Map<String, String> generateEmailReply(EmailRequest request) {
        final String language = resolveLanguage(request.getLanguage());
        String prompt = buildSingleReplyPrompt(request, language);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature",     0.7,
                        "maxOutputTokens", 1024,
                        "topP",            0.8,
                        "topK",            40
                )
        );

        String raw = callGemini(requestBody);
        String response = extractResponseContent(raw);

        String summary = "";
        String reply   = "";

        if (response != null && !response.isBlank()) {
            String[] parts = response.split("(?i)Reply:");
            if (parts.length > 0) {
                summary = parts[0].replaceFirst("(?i)Summary:\\s*", "").trim();
                if (parts.length > 1) reply = parts[1].trim();
            }
        }

        if (summary.isEmpty()) summary = "Summary not available";
        if (reply.isEmpty())   reply   = response;

        return Map.of("summary", summary, "reply", reply);
    }

    /* ── HTTP call ────────────────────────────────────────────────────── */

    private String callGemini(Map<String, Object> requestBody) {
        String url = geminiApiUrl + geminiApiEndpoint + "?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            throw new RuntimeException("Gemini API returned status " + response.getStatusCode());
        } catch (Exception ex) {
            log.error("Gemini API call failed: {}", ex.getMessage());
            throw new RuntimeException("Failed to generate email replies. Please try again.", ex);
        }
    }

    /* ── Prompt builders ─────────────────────────────────────────────── */

    private String buildMultipleRepliesPrompt(EmailRequest request, String language, boolean regenerate) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert email assistant. You must generate exactly 3 different professional email replies and 1 summary.\n");
        prompt.append("IMPORTANT: Generate the reply in ").append(language).append(" language.\n\n");

        if (request.getTone() != null && !request.getTone().isBlank()) {
            prompt.append("Use a ").append(request.getTone()).append(" tone for all replies.\n\n");
        }

        if (regenerate) {
            prompt.append("IMPORTANT: Generate completely new variations different from previous ones. Timestamp: ")
                    .append(Instant.now()).append("\n\n");
        }

        prompt.append("Original Email Subject: ").append(request.getSubject()).append("\n");
        prompt.append("Original Email Content:\n").append(request.getEmailContent()).append("\n\n");
        prompt.append("You MUST follow this EXACT format:\n\n");
        prompt.append("SUMMARY: [Brief 1-2 sentence summary]\n\n");
        prompt.append("REPLY 1: [First variation - 3-5 sentences]\n\n");
        prompt.append("REPLY 2: [Second variation - 3-5 sentences]\n\n");
        prompt.append("REPLY 3: [Third variation - 3-5 sentences]\n\n");
        prompt.append("CRITICAL RULES:\n");
        prompt.append("- Start each section with exact labels: SUMMARY:, REPLY 1:, REPLY 2:, REPLY 3:\n");
        prompt.append("- Make each reply distinctly different in approach or style\n");
        prompt.append("- Do not include email signatures or greetings — just body content\n");
        return prompt.toString();
    }

    private String buildSingleReplyPrompt(EmailRequest request, String language) {
        return String.format(
                "You are a professional email assistant. Provide a short Summary and a single Reply body in %s.\n"
                        + "Summary: (1-2 sentences)\nReply: (3-5 sentences, body only)\n\n"
                        + "EMAIL:\nSubject: %s\nContent: %s\nTone: %s\n",
                language,
                request.getSubject()      == null ? "" : request.getSubject(),
                request.getEmailContent() == null ? "" : request.getEmailContent(),
                request.getTone()         == null ? "professional" : request.getTone()
        );
    }

    /* ── Response parsing ────────────────────────────────────────────── */

    private Map<String, Object> parseMultipleRepliesResponse(String raw) {
        try {
            String content = extractResponseContent(raw);
            String summary = "";
            List<String> replies = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            String mode = "none";

            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.toLowerCase().startsWith("summary:")) {
                    if (current.length() > 0 && mode.startsWith("reply")) {
                        replies.add(current.toString().trim()); current.setLength(0);
                    }
                    summary = line.replaceFirst("(?i)summary:\\s*", "").trim();
                    mode = "summary";
                } else if (line.toLowerCase().matches("reply\\s*[123]:.*")) {
                    if (current.length() > 0 && mode.startsWith("reply")) {
                        replies.add(current.toString().trim()); current.setLength(0);
                    }
                    mode = "reply";
                    String text = line.replaceFirst("(?i)reply\\s*[123]:\\s*", "").trim();
                    if (!text.isEmpty()) current.append(text);
                } else if (!line.isEmpty()) {
                    if (mode.equals("summary")) {
                        if (!summary.isEmpty()) summary += " ";
                        summary += line;
                    } else if (mode.startsWith("reply")) {
                        if (current.length() > 0) current.append(" ");
                        current.append(line);
                    }
                }
            }

            if (current.length() > 0 && mode.startsWith("reply")) replies.add(current.toString().trim());
            while (replies.size() < 3) replies.add("Thank you for your message. I will review and respond shortly.");
            if (replies.size() > 3)  replies = replies.subList(0, 3);
            if (summary.isEmpty())   summary = "Generated professional email responses based on the provided content.";
            summary = summary.trim();
            if (summary.length() > 200) summary = summary.substring(0, 197) + "...";

            return Map.of("replies", replies, "summary", summary);

        } catch (Exception ex) {
            log.error("Failed to parse Gemini response: {}", ex.getMessage());
            return Map.of(
                    "summary", "Failed to parse response. Please try again.",
                    "replies", List.of(
                            "Thank you for your message. I will review and respond shortly.",
                            "I appreciate you reaching out. I will consider and respond soon.",
                            "I've received your email and will follow up soon."
                    )
            );
        }
    }

    private String extractResponseContent(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            JsonNode root = mapper.readTree(raw);
            if (root.has("candidates")) {
                JsonNode parts = root.path("candidates").get(0)
                        .path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    JsonNode textNode = parts.get(0).path("text");
                    if (!textNode.isMissingNode()) return textNode.asText().trim();
                }
            }
            return root.has("output") ? root.path("output").asText().trim() : raw;
        } catch (Exception ex) {
            log.warn("Unable to parse Gemini JSON response, using raw string");
            return raw;
        }
    }

    private String resolveLanguage(String language) {
        return (language == null || language.isBlank()) ? "en" : language;
    }
}