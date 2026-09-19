package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.model.Trip;
import com.example.distribution_backernd.repository.TripRepository;
import com.example.distribution_backernd.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/trips")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ManagerTripController {
    private final TripRepository tripRepo;
    private final JwtUtil jwtUtil;

    @GetMapping("/{tripId}")
    public Trip getTrip(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer tripId) {

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return tripRepo.findByIdAndFleetId(tripId, fleetId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
    }
}
