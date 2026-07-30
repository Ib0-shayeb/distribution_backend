package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.User;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.repository.UserRepository;
import com.example.distribution_backernd.service.LocationStreamService;
import lombok.RequiredArgsConstructor;
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

    private final LocationLogRepository logRepo;
    private final UserRepository userRepo;
    private final LocationStreamService streamService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }

    @PostMapping("/log")
    public LocationLog logLocation(@RequestBody LocationLog newLog, Authentication authentication) {
        String username = authentication.getName();

        User driver = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Driver not found with username: " + username));

        newLog.setUserId(driver.getId());
        if (newLog.getRecordedAt() == null) {
            newLog.setRecordedAt(ZonedDateTime.now());
        }

        LocationLog savedLog = logRepo.save(newLog);
        streamService.broadcastLocation(savedLog);

        return savedLog;
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

        ZonedDateTime now = ZonedDateTime.now();

        for (LocationLog log : logs) {
            log.setUserId(driver.getId());
            if (log.getRecordedAt() == null) {
                log.setRecordedAt(now);
            }
        }

        List<LocationLog> savedLogs = logRepo.saveAll(logs);

        if (!savedLogs.isEmpty()) {
            streamService.broadcastLocation(savedLogs.get(savedLogs.size() - 1));
        }

        return ResponseEntity.ok("Synced " + savedLogs.size() + " location records.");
    }
}