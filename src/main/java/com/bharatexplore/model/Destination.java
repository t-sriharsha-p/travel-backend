package com.bharatexplore.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "destinations")
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String state;

    /** North, South, East, West, Central or Northeast. */
    @Column(nullable = false, length = 30)
    private String region;

    @Column(length = 200)
    private String tagline;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "best_time_to_visit", length = 200)
    private String bestTimeToVisit;

    /** Comma separated interests, e.g. "Heritage,Food,Shopping". */
    @Column(length = 300)
    private String tags;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TouristPlace> places = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Hotel> hotels = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Restaurant> restaurants = new ArrayList<>();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBestTimeToVisit() { return bestTimeToVisit; }
    public void setBestTimeToVisit(String bestTimeToVisit) { this.bestTimeToVisit = bestTimeToVisit; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public List<TouristPlace> getPlaces() { return places; }
    public List<Hotel> getHotels() { return hotels; }
    public List<Restaurant> getRestaurants() { return restaurants; }
}
