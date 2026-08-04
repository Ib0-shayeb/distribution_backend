package com.example.distribution_backernd.dto;

import java.util.List;

public record DriverTripHistory(
        Integer userId,
        List<TripHistory> trips
) {}