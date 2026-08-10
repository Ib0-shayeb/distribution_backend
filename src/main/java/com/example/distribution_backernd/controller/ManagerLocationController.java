package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.dto.DriverTripHistory;
import com.example.distribution_backernd.dto.TripHistory;
import com.example.distribution_backernd.model.*;
import com.example.distribution_backernd.repository.*;
import com.example.distribution_backernd.security.JwtUtil;
import com.example.distribution_backernd.service.LocationLogOrganisation;
import com.example.distribution_backernd.service.LocationStreamService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/locations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ManagerLocationController {
    private final LocationLogOrganisation logOrg;
    private final LocationLogRepository logRepo;
    private final UserRepository userRepo;
    private final ChecklistRepository checklistRepo;
    private final ChecklistItemRepository checklistItemRepo;
    private final LocationStreamService streamService;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepo;
    private final JwtUtil jwtUtil;

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }

    @GetMapping("/history")
    public ResponseEntity<List<DriverTripHistory>> getHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam List<Integer> userIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end) {

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        List<DriverTripHistory> history = logOrg.getBatchDriverTripHistory(fleetId, userIds, start, end);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/active")
    public List<Integer> getActiveDrivers(
            @RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return logRepo.findActiveUserIds(fleetId);
    }

    @GetMapping("/all-workers")
    public List<User> getAllWorkers(
            @RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return userRepo.findByFleetId(fleetId);
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter streamDriverLocation(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Integer userId,
            HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return streamService.createStream(fleetId, userId);
    }

    @PostMapping("/register-worker")
    public ResponseEntity<?> registerWorker(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String phoneNumber = payload.get("phoneNumber");
        String username = payload.get("username");
        String rawPassword = payload.get("password");

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Driver name is required.");
        }
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username is required.");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Password is required.");
        }

        if (userRepo.findByUsername(username.trim()).isPresent()) {
            return ResponseEntity.badRequest().body("Username '" + username + "' is already taken.");
        }

        User worker = new User();
        worker.setName(name.trim());
        worker.setFleetId(fleetId);
        worker.setPhoneNumber(phoneNumber);
        worker.setUsername(username.trim());
        worker.setPasswordHash(passwordEncoder.encode(rawPassword));
        worker.setEnabled(true);

        User savedWorker = userRepo.save(worker);

        Authority driverAuth = new Authority(savedWorker.getUsername(), "ROLE_DRIVER");
        authorityRepo.save(driverAuth);

        return ResponseEntity.ok("Driver registered successfully with ID: " + savedWorker.getId());
    }

    @PostMapping("/create-checklist")
    public ResponseEntity<?> registerWorker(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String name) {

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        Checklist checklist = new Checklist(name, fleetId);

        return ResponseEntity.ok("Checklist added successfully with ID: " + checklist.getId());
    }

    @GetMapping("/all-checklists")
    public List<Checklist> getAllChecklists(
            @RequestHeader("Authorization") String authHeader) {

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return checklistRepo.findByFleetId(fleetId);
    }

    @GetMapping("/checklist/{checklistId}/items")
    public List<ChecklistItem> getChecklistItems(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return checklistItemRepo.findByChecklistIdAndFleetId(checklistId, fleetId);
    }

    @PostMapping("/checklist/{checklistId}/add-items")
    public ResponseEntity<?> addChecklistItems(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId, @RequestBody List<ChecklistItem> items) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        Checklist checklist = checklistRepo.findByIdAndFleetId(checklistId, fleetId)
                .orElseThrow(() -> new RuntimeException("Checklist with ID: " + checklistId + " does not exist"));

        checklistItemRepo.saveAll(items);

        return ResponseEntity.ok("Synced " + items.size() + " item records.");
    }

    @PostMapping("/checklist/{checklistId}/delete-item/{itemId}")
    public ResponseEntity<?> deleteChecklistItems(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId,  @PathVariable Integer itemId) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        Checklist checklist = checklistRepo.findByIdAndFleetId(checklistId, fleetId)
                .orElseThrow(() -> new RuntimeException("Checklist with ID: " + checklistId + " does not exist"));

        checklistItemRepo.deleteByIdAndChecklistId(itemId, checklistId);

        return ResponseEntity.ok("Removed item.");
    }

    @PostMapping("checklist/{checklistId}/assign-driver/{driverId}")
    public ResponseEntity<?> assignDriver(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId,  @PathVariable Integer driverId) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        Checklist checklist = checklistRepo.findByIdAndFleetId(checklistId, fleetId)
                .orElseThrow(() -> new RuntimeException("Checklist with ID: " + checklistId + " does not exist"));

        checklist.setDriverId(driverId);

        return ResponseEntity.ok("Assigned " + driverId + " to " + checklist.getId());
    }
}
