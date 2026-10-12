package com.bharatexplore.controller;

import com.bharatexplore.dto.PlannerDtos.PlanRequest;
import com.bharatexplore.dto.PlannerDtos.PlanResponse;
import com.bharatexplore.dto.PlannerDtos.PlanSummary;
import com.bharatexplore.service.PlanService;
import com.bharatexplore.service.PlannerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PlannerController {

    private final PlannerService planner;
    private final PlanService plans;

    public PlannerController(PlannerService planner, PlanService plans) {
        this.planner = planner;
        this.plans = plans;
    }

    /** Generates and saves a personalised itinerary for the logged-in user. */
    @PostMapping("/planner/generate")
    public PlanResponse generate(@Valid @RequestBody PlanRequest req, Authentication authentication) {
        return planner.generate(authentication.getName(), req);
    }

    @GetMapping("/plans")
    public List<PlanSummary> myPlans(Authentication authentication) {
        return plans.list(authentication.getName());
    }

    @GetMapping("/plans/{id}")
    public PlanResponse plan(@PathVariable Long id, Authentication authentication) {
        return plans.get(authentication.getName(), id);
    }

    @DeleteMapping("/plans/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        plans.delete(authentication.getName(), id);
    }
}
