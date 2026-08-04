package com.example.distribution_backernd.dto;

import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.TripStatus;

import java.time.ZonedDateTime;
import java.util.List;

public record TripHistory(
        Integer tripId,
        List<LocationLogDTO> logs
) {}