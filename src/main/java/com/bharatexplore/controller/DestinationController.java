package com.bharatexplore.controller;

import com.bharatexplore.dto.DestinationDtos.DestinationDetail;
import com.bharatexplore.dto.DestinationDtos.DestinationSummary;
import com.bharatexplore.service.DestinationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationService service;

    public DestinationController(DestinationService service) {
        this.service = service;
    }

    /** Search by name, state or interest tag; optionally filter by region. */
    @GetMapping
    public List<DestinationSummary> search(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String region) {
        return service.search(q, region);
    }

    @GetMapping("/{id}")
    public DestinationDetail get(@PathVariable Long id) {
        return service.getById(id);
    }
}
