package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.Trip;
import com.example.distribution_backernd.model.TripStatus;
import com.example.distribution_backernd.model.User;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.repository.TripRepository;
import com.example.distribution_backernd.repository.UserRepository;
import com.example.distribution_backernd.security.JwtUtil;
import com.example.distribution_backernd.service.LocationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/driver/locations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DriverLocationController {
    private final TripRepository tripRepo;
    private final LocationLogRepository logRepo;
    private final UserRepository userRepo;
    private final LocationStreamService streamService;
    private final JwtUtil jwtUtil;

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }

    @PostMapping("/start")
    public ResponseEntity<?> startTrip(@RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);
        Integer userId = jwtUtil.extractUserId(jwt);

        Trip trip = new Trip(userId, fleetId, ZonedDateTime.now());

        Trip savedTrip = tripRepo.save(trip);

        return ResponseEntity.ok(savedTrip.getId());
    }

    @PostMapping("/end/{tripId}")
    public ResponseEntity<?> endTrip(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer tripId) {

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);
        Integer userId = jwtUtil.extractUserId(jwt);

        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));
        // check if trip belongs to this user
        if (!trip.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Trip not found");
        }

        if (trip.getStatus() != TripStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trip is no longer active");
        }

        trip.setStatus(TripStatus.COMPLETED);
        trip.setEndedAt(ZonedDateTime.now());
        trip.setFleetId(fleetId);
        Trip savedTrip = tripRepo.save(trip);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/log")
    public ResponseEntity<?> logLocation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LocationLog newLog, Authentication authentication) {

        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);

        if (newLog.getTripId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("tripId is required");
        }
        Trip trip = tripRepo.findById(newLog.getTripId())
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + newLog.getTripId()));
        // check if trip belongs to this user
        if (!trip.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Trip not found");
        }

        if (trip.getStatus() != TripStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trip is no longer active");
        }

        if (newLog.getRecordedAt() == null) {
            newLog.setRecordedAt(ZonedDateTime.now());
        }

        LocationLog savedLog = logRepo.save(newLog);
        streamService.broadcastLocation(savedLog, userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-log")
    public ResponseEntity<?> logLocationBatch(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody List<LocationLog> logs) {

        if (logs == null || logs.isEmpty()) {
            return ResponseEntity.ok("No logs to sync.");
        }

        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);

        Integer tripId = logs.getFirst().getTripId();
        if (tripId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("tripId is required");
        }
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        // check if trip belongs to this user
        if (!trip.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Trip not found");
        }
        if (trip.getStatus() != TripStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trip is no longer active");
        }

        for (LocationLog log : logs) {
            if (log.getTripId() == null || !log.getTripId().equals(tripId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Varying or missing trip Ids");
            }
            if (log.getRecordedAt() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("log time is required");
            }
        }

        List<LocationLog> savedLogs = logRepo.saveAll(logs);

        if (!savedLogs.isEmpty()) {
            streamService.broadcastLocation(savedLogs.get(savedLogs.size() - 1), userId);
        }

        return ResponseEntity.ok("Synced " + savedLogs.size() + " location records.");
    }
}