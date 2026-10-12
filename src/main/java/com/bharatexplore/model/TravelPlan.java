package com.bharatexplore.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "travel_plans")
public class TravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "destination_name", nullable = false, length = 100)
    private String destinationName;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "trip_days", nullable = false)
    private int tripDays;

    /** Source of the plan: "gemini" or "rule-based". */
    @Column(nullable = false, length = 20)
    private String source;

    @Lob
    @Column(name = "preferences_json", nullable = false)
    private String preferencesJson;

    @Lob
    @Column(name = "plan_json", nullable = false)
    private String planJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getTripDays() { return tripDays; }
    public void setTripDays(int tripDays) { this.tripDays = tripDays; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPreferencesJson() { return preferencesJson; }
    public void setPreferencesJson(String preferencesJson) { this.preferencesJson = preferencesJson; }
    public String getPlanJson() { return planJson; }
    public void setPlanJson(String planJson) { this.planJson = planJson; }
    public Instant getCreatedAt() { return createdAt; }
}
