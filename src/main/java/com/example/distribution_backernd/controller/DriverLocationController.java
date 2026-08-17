package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.dto.ChecklistWithItemsDTO;
import com.example.distribution_backernd.dto.LocationLogDTO;
import com.example.distribution_backernd.dto.LocationScanResponseDTO;
import com.example.distribution_backernd.model.*;
import com.example.distribution_backernd.repository.*;
import com.example.distribution_backernd.security.JwtUtil;
import com.example.distribution_backernd.service.LocationStreamService;
import com.example.distribution_backernd.util.LocationUtils;
import jakarta.persistence.Index;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

@RestController
@RequestMapping("/api/driver/locations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DriverLocationController {
    private final TripRepository tripRepo;
    private final LocationLogRepository logRepo;
    private final UserRepository userRepo;
    private final ChecklistRepository checklistRepo;
    private final ChecklistItemRepository checklistItemRepo;
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

    @PostMapping("/scan")
    public ResponseEntity<?> scanLocation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LocationLogDTO location) {
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

        for (Checklist checklist : checklists) {
            boolean allChecklistItemsCompleted = true;
            for (ChecklistItem item : checklist.getItems()) {
                double distance = LocationUtils.distanceInMeters(item.getLatitude(), item.getLongitude(), location.latitude(), location.longitude());
                if (item.getCompletedAt() == null && distance < GEOFENCE_RADIUS_METERS) {
                    item.setCompletedAt(LocalDateTime.now());
                    checklistItemRepo.save(item);
                    completedItems.add(item);
                }
                if (item.getCompletedAt() == null) {allChecklistItemsCompleted = false;}
            }
            if (checklist.getCompletedAt() == null && allChecklistItemsCompleted) {
                checklist.setCompletedAt(LocalDateTime.now());
                checklistRepo.save(checklist);
                completedChecklists.add(checklist);
            }
        }

        List<ChecklistWithItemsDTO> updatedChecklistWithItems = checklists.stream()
                .map(c -> new ChecklistWithItemsDTO(c, c.getItems()))
                .toList();

        return ResponseEntity.ok(new LocationScanResponseDTO(
                completedItems,
                completedChecklists,
                updatedChecklistWithItems
        ));
    }

    @GetMapping("/assigned-checklists")
    public List<Checklist> assignedChecklist(@RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);

        return checklistRepo.findByDriverId(userId);
    }

    @GetMapping("/assigned-checklists-with-items")
    public List<ChecklistWithItemsDTO> assignedChecklistWithItems(@RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        List<Checklist> checklists = checklistRepo.findByFleetIdAndDriverIdWithItems(fleetId, userId);

        return checklists.stream()
                .map(c -> new ChecklistWithItemsDTO(c, c.getItems()))
                .toList();
    }

    @GetMapping("/assigned-checklists/{checklistId}")
    public List<ChecklistItem> assignedChecklist(@RequestHeader("Authorization") String authHeader
            , @PathVariable Integer checklistId) {
        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        Checklist checklist = checklistRepo.findByIdAndDriverId(checklistId, userId)
                .orElseThrow(() -> new RuntimeException("Checklist doesent exist"));
        return checklistItemRepo.findByChecklistId(checklistId);
    }

    @PostMapping("/checklist/{checklistId}/add-items")
    public ResponseEntity<?> addChecklistItems(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId, @RequestBody List<ChecklistItem> items) {
        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);

        Checklist checklist = checklistRepo.findByIdAndDriverId(checklistId, userId)
                .orElseThrow(() -> new RuntimeException("Checklist does not exist"));

        for (ChecklistItem item : items) {
            item.setChecklist(checklist);
            item.setAddedById(userId);
        }

        checklistItemRepo.saveAll(items);

        return ResponseEntity.ok("Synced " + items.size() + " item records.");
    }

    @DeleteMapping("/checklist/{checklistId}/delete-item/{itemId}")
    public ResponseEntity<?> deleteChecklistItems(
            @RequestHeader("Authorization") String authHeader, @PathVariable Integer itemId, @PathVariable Integer checklistId) {
        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);

        long deletedCount = checklistItemRepo.deleteByIdAndAddedById(itemId, userId);

        if (deletedCount == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to delete this item or it does not exist.");
        }

        return ResponseEntity.ok("Removed item.");
    }
}