package com.bharatexplore.config;

import com.bharatexplore.model.*;
import com.bharatexplore.repository.DestinationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/** Loads destination, place, hotel and restaurant data from seed-data.json when the database is empty. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    record PlaceSeed(String name, String description, String category, String visitDuration) {}
    record HotelSeed(String name, String description, String priceCategory, Integer approxPricePerNight) {}
    record RestaurantSeed(String name, String cuisine, String description, String specialty,
                          String foodType, String priceCategory) {}
    record DestinationSeed(String name, String state, String region, String tagline, String description,
                           String bestTimeToVisit, String tags, List<PlaceSeed> places,
                           List<HotelSeed> hotels, List<RestaurantSeed> restaurants) {}

    private final DestinationRepository repo;
    private final ObjectMapper mapper;

    public DataSeeder(DestinationRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (repo.count() > 0) return;
        try (InputStream in = new ClassPathResource("seed-data.json").getInputStream()) {
            DestinationSeed[] seeds = mapper.readValue(in, DestinationSeed[].class);
            for (DestinationSeed s : seeds) {
                Destination d = new Destination();
                d.setName(s.name());
                d.setState(s.state());
                d.setRegion(s.region());
                d.setTagline(s.tagline());
                d.setDescription(s.description());
                d.setBestTimeToVisit(s.bestTimeToVisit());
                d.setTags(s.tags());
                for (PlaceSeed p : s.places()) {
                    TouristPlace tp = new TouristPlace();
                    tp.setName(p.name());
                    tp.setDescription(p.description());
                    tp.setCategory(p.category());
                    tp.setVisitDuration(p.visitDuration());
                    tp.setDestination(d);
                    d.getPlaces().add(tp);
                }
                for (HotelSeed h : s.hotels()) {
                    Hotel hotel = new Hotel();
                    hotel.setName(h.name());
                    hotel.setDescription(h.description());
                    hotel.setPriceCategory(h.priceCategory());
                    hotel.setApproxPricePerNight(h.approxPricePerNight());
                    hotel.setDestination(d);
                    d.getHotels().add(hotel);
                }
                for (RestaurantSeed r : s.restaurants()) {
                    Restaurant rest = new Restaurant();
                    rest.setName(r.name());
                    rest.setCuisine(r.cuisine());
                    rest.setDescription(r.description());
                    rest.setSpecialty(r.specialty());
                    rest.setFoodType(r.foodType());
                    rest.setPriceCategory(r.priceCategory());
                    rest.setDestination(d);
                    d.getRestaurants().add(rest);
                }
                repo.save(d);
            }
            log.info("Seeded {} destinations", seeds.length);
        }
    }
}
