package com.example.distribution_backernd.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "location_logs")
public class LocationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @Column(name = "trip_id",  nullable = false)
    private Integer tripId;

    @Column(nullable = false)
    private Double latitude;
    @Column(nullable = false)
    private Double longitude;

    @Column(name = "recorded_at", nullable = false)
    private ZonedDateTime recordedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getTripId() { return tripId; }
    public void setTripId(Integer userId) { this.tripId = userId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public ZonedDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(ZonedDateTime recordedAt) { this.recordedAt = recordedAt; }
}
