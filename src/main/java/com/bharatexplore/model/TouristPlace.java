package com.bharatexplore.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tourist_places")
public class TouristPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    /** One of the interest categories: Heritage, Nature, Adventure, Spiritual, Food, Beaches, Shopping, Nightlife, Wildlife, Relaxation. */
    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "visit_duration", length = 60)
    private String visitDuration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVisitDuration() { return visitDuration; }
    public void setVisitDuration(String visitDuration) { this.visitDuration = visitDuration; }
    public Destination getDestination() { return destination; }
    public void setDestination(Destination destination) { this.destination = destination; }
}
