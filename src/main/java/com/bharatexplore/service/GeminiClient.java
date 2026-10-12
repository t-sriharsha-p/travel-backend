package com.bharatexplore.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thin wrapper around the Google Gemini generateContent REST endpoint. */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient client;
    private final String apiKey;
    private final String model;

    public GeminiClient(@Value("${app.gemini.api-key}") String apiKey,
                        @Value("${app.gemini.model}") String model,
                        @Value("${app.gemini.base-url}") String baseUrl,
                        @Value("${app.gemini.timeout-seconds}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    /** Returns the model's JSON text, or empty if the call fails for any reason. */
    public Optional<String> generateJson(String prompt) {
        if (!isEnabled()) return Optional.empty();
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.7));
        try {
            JsonNode response = client.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) return Optional.empty();
            JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) sb.append(part.get("text").asText());
            }
            String text = sb.toString().trim();
            return text.isEmpty() ? Optional.empty() : Optional.of(stripFences(text));
        } catch (Exception ex) {
            // Never log the API key; the exception message from RestClient does not contain headers.
            log.warn("Gemini request failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String stripFences(String text) {
        String t = text;
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline > 0) t = t.substring(firstNewline + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }
}
