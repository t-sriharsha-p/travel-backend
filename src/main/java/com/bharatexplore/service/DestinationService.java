package com.bharatexplore.service;

import com.bharatexplore.dto.DestinationDtos.*;
import com.bharatexplore.exception.ApiException;
import com.bharatexplore.model.Destination;
import com.bharatexplore.repository.DestinationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DestinationService {

    private final DestinationRepository repo;

    public DestinationService(DestinationRepository repo) {
        this.repo = repo;
    }

    public List<DestinationSummary> search(String q, String region) {
        String query = q == null ? null : q.trim();
        String reg = region == null ? null : region.trim();
        return repo.search(query, reg).stream().map(this::toSummary).toList();
    }

    public DestinationDetail getById(Long id) {
        return repo.findById(id).map(this::toDetail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Destination not found"));
    }

    public Optional<DestinationDetail> findByName(String name) {
        return repo.findByNameIgnoreCase(name.trim()).map(this::toDetail);
    }

    public List<String> allNames() {
        return repo.findAll().stream().map(Destination::getName).sorted().toList();
    }

    private List<String> tags(Destination d) {
        if (d.getTags() == null || d.getTags().isBlank()) return List.of();
        return Arrays.stream(d.getTags().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private DestinationSummary toSummary(Destination d) {
        return new DestinationSummary(d.getId(), d.getName(), d.getState(), d.getRegion(),
                d.getTagline(), d.getBestTimeToVisit(), tags(d));
    }

    private DestinationDetail toDetail(Destination d) {
        return new DestinationDetail(d.getId(), d.getName(), d.getState(), d.getRegion(), d.getTagline(),
                d.getDescription(), d.getBestTimeToVisit(), tags(d),
                d.getPlaces().stream().map(p -> new PlaceDto(p.getId(), p.getName(), p.getDescription(),
                        p.getCategory(), p.getVisitDuration())).toList(),
                d.getHotels().stream().map(h -> new HotelDto(h.getId(), h.getName(), h.getDescription(),
                        h.getPriceCategory(), h.getApproxPricePerNight())).toList(),
                d.getRestaurants().stream().map(r -> new RestaurantDto(r.getId(), r.getName(), r.getCuisine(),
                        r.getDescription(), r.getSpecialty(), r.getFoodType(), r.getPriceCategory())).toList());
    }
}
