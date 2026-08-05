package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.Trip;
import com.example.distribution_backernd.model.TripStatus;
import com.example.distribution_backernd.model.User;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.repository.TripRepository;
import com.example.distribution_backernd.repository.UserRepository;
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

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }

    @PostMapping("/start")
    public ResponseEntity<?> startTrip(Authentication authentication) {
        String username = authentication.getName();

        User driver = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Driver not found with username: " + username));

        Trip trip = new Trip(driver.getId(), ZonedDateTime.now());

        Trip savedTrip = tripRepo.save(trip);

        return ResponseEntity.ok(savedTrip.getId());
    }

    @PostMapping("/end/{tripId}")
    public ResponseEntity<?> endTrip(@PathVariable Integer tripId, Authentication authentication) {
        String username = authentication.getName();

        User driver = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Driver not found with username: " + username));

        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));
        // check if trip belongs to this user
        if (!trip.getUserId().equals(driver.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Trip not found");
        }

        if (trip.getStatus() != TripStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trip is no longer active");
        }

        trip.setStatus(TripStatus.COMPLETED);
        trip.setEndedAt(ZonedDateTime.now());
        Trip savedTrip = tripRepo.save(trip);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/log")
    public ResponseEntity<?> logLocation(@RequestBody LocationLog newLog, Authentication authentication) {
        String username = authentication.getName();

        User driver = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Driver not found with username: " + username));

        if (newLog.getTripId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("tripId is required");
        }
        Trip trip = tripRepo.findById(newLog.getTripId())
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + newLog.getTripId()));
        // check if trip belongs to this user
        if (!trip.getUserId().equals(driver.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Trip not found");
        }

        if (trip.getStatus() != TripStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trip is no longer active");
        }

        if (newLog.getRecordedAt() == null) {
            newLog.setRecordedAt(ZonedDateTime.now());
        }

        LocationLog savedLog = logRepo.save(newLog);
        streamService.broadcastLocation(savedLog, driver.getId());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-log")
    public ResponseEntity<?> logLocationBatch(
            @RequestBody List<LocationLog> logs,
            Authentication authentication) {

        if (logs == null || logs.isEmpty()) {
            return ResponseEntity.ok("No logs to sync.");
        }

        String username = authentication.getName();
        User driver = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Driver not found with username: " + username));


        Integer tripId = logs.getFirst().getTripId();
        if (tripId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("tripId is required");
        }
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        // check if trip belongs to this user
        if (!trip.getUserId().equals(driver.getId())) {
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
            streamService.broadcastLocation(savedLogs.get(savedLogs.size() - 1), driver.getId());
        }

        return ResponseEntity.ok("Synced " + savedLogs.size() + " location records.");
    }
}