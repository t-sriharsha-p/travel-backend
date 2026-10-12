package com.bharatexplore.service;

import com.bharatexplore.dto.PlannerDtos.PlanResponse;
import com.bharatexplore.dto.PlannerDtos.PlanSummary;
import com.bharatexplore.exception.ApiException;
import com.bharatexplore.model.TravelPlan;
import com.bharatexplore.model.User;
import com.bharatexplore.repository.TravelPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanService {

    private final TravelPlanRepository repo;
    private final AuthService auth;
    private final ObjectMapper mapper;

    public PlanService(TravelPlanRepository repo, AuthService auth, ObjectMapper mapper) {
        this.repo = repo;
        this.auth = auth;
        this.mapper = mapper;
    }

    @Transactional
    public PlanResponse save(User user, String destination, int days, String source,
                             JsonNode preferences, JsonNode itinerary) {
        TravelPlan plan = new TravelPlan();
        plan.setUser(user);
        plan.setDestinationName(destination);
        plan.setTripDays(days);
        plan.setSource(source);
        String title = itinerary.path("title").asText(days + "-Day " + destination + " Trip");
        plan.setTitle(title.length() > 200 ? title.substring(0, 200) : title);
        try {
            plan.setPreferencesJson(mapper.writeValueAsString(preferences));
            plan.setPlanJson(mapper.writeValueAsString(itinerary));
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the plan");
        }
        repo.save(plan);
        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanSummary> list(String email) {
        Long userId = auth.requireUser(email).getId();
        return repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(p -> new PlanSummary(p.getId(), p.getDestinationName(), p.getTitle(), p.getTripDays(),
                        p.getSource(), p.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse get(String email, Long id) {
        Long userId = auth.requireUser(email).getId();
        return repo.findByIdAndUserId(id, userId).map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plan not found"));
    }

    @Transactional
    public void delete(String email, Long id) {
        Long userId = auth.requireUser(email).getId();
        TravelPlan plan = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Plan not found"));
        repo.delete(plan);
    }

    private PlanResponse toResponse(TravelPlan p) {
        try {
            return new PlanResponse(p.getId(), p.getDestinationName(), p.getTitle(), p.getTripDays(), p.getSource(),
                    mapper.readTree(p.getPreferencesJson()), mapper.readTree(p.getPlanJson()), p.getCreatedAt());
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored plan is corrupted");
        }
    }
}
