package com.bharatexplore.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class PlannerDtos {
    private PlannerDtos() {}

    /** Preferences collected by the planner form on the React frontend. */
    public record PlanRequest(
            @NotBlank(message = "Destination is required") @Size(max = 100) String destination,
            @Min(value = 1, message = "Trip must be at least 1 day") @Max(value = 14, message = "Trip can be at most 14 days") int days,
            @Min(value = 1000, message = "Budget must be at least 1000 INR") int budget,
            List<@Size(max = 40) String> interests,
            /** VEG, NON_VEG or ANY. */
            String foodPreference,
            /** e.g. Relaxed, Balanced, Packed, Luxury, Backpacker. */
            @Size(max = 40) String travelStyle) {}

    /** The itinerary is kept as free-form JSON so the AI output shape can evolve without schema changes. */
    public record PlanResponse(Long id, String destination, String title, int days, String source,
                               JsonNode preferences, JsonNode itinerary, Instant createdAt) {}

    public record PlanSummary(Long id, String destination, String title, int days, String source, Instant createdAt) {}
}
