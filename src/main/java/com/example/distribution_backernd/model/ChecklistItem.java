package com.example.distribution_backernd.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String description;

    @Column(name = "google_place_id")
    private String googlePlaceId;

    @Column(name = "added_by_id", nullable = false)
    private Integer addedById;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Double latitude;
    @Column(nullable = false)
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    @JsonIgnore // Prevents circular reference during serialization
    private Checklist checklist;

    public ChecklistItem() {}
    public ChecklistItem(Integer checklistId, Double latitude, Double longitude, Integer addedById, String name, String description) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.addedById = addedById;
        this.name = name;
        this.description = description;
    }
    public ChecklistItem(Integer checklistId, Double latitude, Double longitude, Integer addedById, String name, String description, String googlePlaceId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.addedById = addedById;
        this.googlePlaceId = googlePlaceId;
        this.name = name;
        this.description = description;
    }

    public Integer getAddedById() { return addedById; }
    public void setAddedById(Integer addedById) { this.addedById = addedById; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getGooglePlaceId() { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId) { this.googlePlaceId = googlePlaceId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Checklist getChecklist() {return checklist;}
    public void setChecklist(Checklist checklist) {this.checklist = checklist;}
}
