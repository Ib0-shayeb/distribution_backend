package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.dto.ChecklistWithItemsDTO;
import com.example.distribution_backernd.dto.LocationDTO;
import com.example.distribution_backernd.dto.LocationScanResponseDTO;
import com.example.distribution_backernd.model.Checklist;
import com.example.distribution_backernd.model.ChecklistItem;
import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.Trip;
import com.example.distribution_backernd.model.TripStatus;
import com.example.distribution_backernd.repository.ChecklistItemRepository;
import com.example.distribution_backernd.repository.ChecklistRepository;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.repository.TripRepository;
import com.example.distribution_backernd.security.JwtUtil;
import com.example.distribution_backernd.service.UserStreamService;
import com.example.distribution_backernd.util.LocationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/driver/trips")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DriverTripController {
    private final TripRepository tripRepo;
    private final LocationLogRepository logRepo;
    private final ChecklistRepository checklistRepo;
    private final ChecklistItemRepository checklistItemRepo;
    private final UserStreamService streamService;
    private final JwtUtil jwtUtil;

    @PostMapping("/start")
    public ResponseEntity<?> startTrip(@RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);
        Integer userId = jwtUtil.extractUserId(jwt);

        Trip trip = new Trip(userId, fleetId, ZonedDateTime.now());

        Trip savedTrip = tripRepo.save(trip);

        return ResponseEntity.ok(savedTrip.getId());
    }

    @PostMapping("/{tripId}/end")
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
        tripRepo.save(trip);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tripId}/locations")
    public ResponseEntity<?> logLocation(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer tripId,
            @RequestBody LocationLog newLog) {

        String jwt = authHeader.substring(7);
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

        newLog.setTripId(tripId);

        if (newLog.getRecordedAt() == null) {
            newLog.setRecordedAt(ZonedDateTime.now());
        }

        LocationLog savedLog = logRepo.save(newLog);
        streamService.broadcastLocation(userId, List.of(savedLog));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tripId}/locations/batch")
    public ResponseEntity<?> logLocationBatch(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer tripId,
            @RequestBody List<LocationLog> logs) {

        if (logs == null || logs.isEmpty()) {
            return ResponseEntity.ok("No logs to sync.");
        }

        String jwt = authHeader.substring(7);
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

        for (LocationLog log : logs) {
            if (log.getTripId() != null && !log.getTripId().equals(tripId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Varying or missing trip Ids");
            }
            if (log.getRecordedAt() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("log time is required");
            }
            log.setTripId(tripId);
        }

        List<LocationLog> savedLogs = logRepo.saveAll(logs);

        if (!savedLogs.isEmpty()) {
            streamService.broadcastLocation(userId, savedLogs);
        }

        return ResponseEntity.ok("Synced " + savedLogs.size() + " location records.");
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scanLocation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LocationDTO location) {
        if (location == null || location.latitude() == null || location.longitude() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Latitude and longitude are required.");
        }

        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        List<Checklist> checklists = checklistRepo.findByFleetIdAndDriverIdWithItems(fleetId, userId);

        List<ChecklistItem> completedItems = new ArrayList<>();
        List<Checklist> completedChecklists = new ArrayList<>();

        final double GEOFENCE_RADIUS_METERS = 40.0;
        LocalDateTime now = LocalDateTime.now();

        for (Checklist checklist : checklists) {
            boolean allChecklistItemsCompleted = true;

            for (ChecklistItem item : checklist.getItems()) {
                if (item.getCompletedAt() == null) {
                    double distance = LocationUtils.distanceInMeters(
                            item.getLatitude(), item.getLongitude(),
                            location.latitude(), location.longitude()
                    );

                    if (distance < GEOFENCE_RADIUS_METERS) {
                        item.setCompletedAt(now);
                        completedItems.add(item);
                    } else {
                        allChecklistItemsCompleted = false;
                    }
                }
            }

            if (checklist.getCompletedAt() == null && allChecklistItemsCompleted) {
                checklist.setCompletedAt(now);
                completedChecklists.add(checklist);
            }
        }

        if (!completedItems.isEmpty()) {
            checklistItemRepo.saveAll(completedItems);
        }
        if (!completedChecklists.isEmpty()) {
            checklistRepo.saveAll(completedChecklists);
        }

        List<ChecklistWithItemsDTO> updatedChecklistWithItems = checklists.stream()
                .map(c -> new ChecklistWithItemsDTO(c, c.getItems()))
                .toList();

        if (!completedItems.isEmpty()) {
            streamService.broadcastChecklistUpdate(userId, updatedChecklistWithItems);
        }

        return ResponseEntity.ok(new LocationScanResponseDTO(
                completedItems,
                completedChecklists,
                updatedChecklistWithItems
        ));
    }
}
