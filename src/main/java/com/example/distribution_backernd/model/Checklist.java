package com.example.distribution_backernd.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checklists")
public class Checklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "fleet_id", nullable = false)
    private Integer fleetId;

    @Column(name = "driver_id")
    private Integer driverId;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(
            mappedBy = "checklist",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ChecklistItem> items = new ArrayList<>();

    public Checklist() {}
    public Checklist(String name, Integer fleetId,  Integer driverId) {
        this.name = name;
        this.fleetId = fleetId;
    }
    public Checklist(String name, Integer fleetId) {
        this.name = name;
        this.fleetId = fleetId;
    }
    public Integer getDriverId() {return driverId;}
    public void setDriverId(Integer driverId) {this.driverId = driverId;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getFleetId() { return fleetId; }
    public void setFleetId(Integer fleetId) { this.fleetId = fleetId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public List<ChecklistItem> getItems() { return items; }
}
