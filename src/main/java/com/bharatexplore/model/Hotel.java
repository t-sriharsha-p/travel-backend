package com.bharatexplore.model;

import jakarta.persistence.*;

@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 600)
    private String description;

    /** BUDGET, MID or LUXURY. */
    @Column(name = "price_category", nullable = false, length = 20)
    private String priceCategory;

    /** Rough price per night in INR. Indicative only. */
    @Column(name = "approx_price_per_night")
    private Integer approxPricePerNight;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriceCategory() { return priceCategory; }
    public void setPriceCategory(String priceCategory) { this.priceCategory = priceCategory; }
    public Integer getApproxPricePerNight() { return approxPricePerNight; }
    public void setApproxPricePerNight(Integer approxPricePerNight) { this.approxPricePerNight = approxPricePerNight; }
    public Destination getDestination() { return destination; }
    public void setDestination(Destination destination) { this.destination = destination; }
}
