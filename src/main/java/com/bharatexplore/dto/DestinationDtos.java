package com.bharatexplore.dto;

import java.util.List;

public final class DestinationDtos {
    private DestinationDtos() {}

    public record DestinationSummary(Long id, String name, String state, String region,
                                     String tagline, String bestTimeToVisit, List<String> tags) {}

    public record PlaceDto(Long id, String name, String description, String category, String visitDuration) {}

    public record HotelDto(Long id, String name, String description, String priceCategory, Integer approxPricePerNight) {}

    public record RestaurantDto(Long id, String name, String cuisine, String description, String specialty,
                                String foodType, String priceCategory) {}

    public record DestinationDetail(Long id, String name, String state, String region, String tagline,
                                    String description, String bestTimeToVisit, List<String> tags,
                                    List<PlaceDto> places, List<HotelDto> hotels, List<RestaurantDto> restaurants) {}
}
