package com.bharatexplore.controller;

import com.bharatexplore.service.GeminiClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final GeminiClient gemini;

    public HealthController(GeminiClient gemini) {
        this.gemini = gemini;
    }

    @GetMapping({"/", "/api/health"})
    public Map<String, Object> health() {
        return Map.of("status", "ok", "service", "bharatexplore-backend", "aiEnabled", gemini.isEnabled());
    }
}
