package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.dto.DriverTripHistory;
import com.example.distribution_backernd.dto.TripHistory;
import com.example.distribution_backernd.model.Authority;
import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.User;
import com.example.distribution_backernd.repository.AuthorityRepository;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.repository.UserRepository;
import com.example.distribution_backernd.service.LocationLogOrganisation;
import com.example.distribution_backernd.service.LocationStreamService;
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
    private final LocationStreamService streamService;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepo;

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }

    @GetMapping("/history")
    public ResponseEntity<List<DriverTripHistory>> getHistory(
            @RequestParam List<Integer> userIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end) {

        List<DriverTripHistory> history = logOrg.getBatchDriverTripHistory(userIds, start, end);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/active")
    public List<Integer> getActiveDrivers() {
        return logRepo.findActiveUserIds();
    }

    @GetMapping("/all-workers")
    public List<User> getAllWorkers() {
        return userRepo.findAll();
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter streamDriverLocation(@RequestParam Integer userId) {
        return streamService.createStream(userId);
    }

    @PostMapping("/register-worker")
    public ResponseEntity<?> registerWorker(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String phoneNumber = payload.get("phoneNumber");
        String username = payload.get("username");
        String rawPassword = payload.get("password");

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
        worker.setPhoneNumber(phoneNumber);
        worker.setUsername(username.trim());
        worker.setPasswordHash(passwordEncoder.encode(rawPassword));
        worker.setEnabled(true);

        User savedWorker = userRepo.save(worker);

        Authority driverAuth = new Authority(savedWorker.getUsername(), "ROLE_DRIVER");
        authorityRepo.save(driverAuth);

        return ResponseEntity.ok("Driver registered successfully with ID: " + savedWorker.getId());
    }
}
