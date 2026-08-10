package com.example.distribution_backernd.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "checklist")
public class Checklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "fleet_id", nullable = false)
    private Integer fleetId;

    @Column(name = "driver_id")
    private Integer driverId;

    public Checklist() {}
    public Checklist(String name, Integer fleetId,  Integer driverId) {
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
}
