package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.dto.ChecklistWithItemsDTO;
import com.example.distribution_backernd.dto.DriverTripHistory;
import com.example.distribution_backernd.model.*;
import com.example.distribution_backernd.repository.*;
import com.example.distribution_backernd.security.JwtUtil;
import com.example.distribution_backernd.service.LocationLogOrganisation;
import com.example.distribution_backernd.service.LocationStreamService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/trips")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ManagerTripsController {
    private final TripRepository tripRepo;
    private final JwtUtil jwtUtil;

    @GetMapping("/{tripId}")
    public Trip getTrip(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer tripId) {

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        Trip trip = tripRepo.findByIdAndFleetId(tripId, fleetId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        return trip;
    }
}
