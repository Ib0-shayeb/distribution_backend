package com.example.distribution_backernd.model;


import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "checklist_id", nullable = false)
    private Integer checklistId;

    @Column(name = "google_place_id")
    private String googlePlaceId;

    @Column(name = "added_by_id", nullable = false)
    private Integer addedById;

    @Column(nullable = false)
    private Double latitude;
    @Column(nullable = false)
    private Double longitude;

    public ChecklistItem() {}
    public ChecklistItem(Integer checklistId, Double latitude, Double longitude, Integer addedById ) {
        this.checklistId = checklistId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.addedById = addedById;
    }
    public ChecklistItem(Integer checklistId, Double latitude, Double longitude, Integer addedById, String googlePlaceId) {
        this.checklistId = checklistId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.addedById = addedById;
        this.googlePlaceId = googlePlaceId;
    }

    public Integer getAddedById() { return addedById; }
    public void setAddedById(Integer addedById) { this.addedById = addedById; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getGooglePlaceId() { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId) { this.googlePlaceId = googlePlaceId; }

    public Integer getChecklistId() { return checklistId; }
    public void setChecklistId(Integer checklistId) { this.checklistId = checklistId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
