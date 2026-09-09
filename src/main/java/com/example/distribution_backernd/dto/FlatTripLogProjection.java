package com.example.distribution_backernd.dto;

import java.time.ZonedDateTime;

public interface FlatTripLogProjection {
    Integer getUserId();
    Integer getTripId();
    Double getLatitude();
    Double getLongitude();
    ZonedDateTime getRecordedAt();
}