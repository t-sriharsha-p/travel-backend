package com.bharatexplore.model;

import jakarta.persistence.*;

@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String cuisine;

    @Column(length = 600)
    private String description;

    @Column(length = 200)
    private String specialty;

    /** VEG, NON_VEG or BOTH. */
    @Column(name = "food_type", nullable = false, length = 20)
    private String foodType;

    /** BUDGET, MID or LUXURY. */
    @Column(name = "price_category", nullable = false, length = 20)
    private String priceCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }
    public String getPriceCategory() { return priceCategory; }
    public void setPriceCategory(String priceCategory) { this.priceCategory = priceCategory; }
    public Destination getDestination() { return destination; }
    public void setDestination(Destination destination) { this.destination = destination; }
}
