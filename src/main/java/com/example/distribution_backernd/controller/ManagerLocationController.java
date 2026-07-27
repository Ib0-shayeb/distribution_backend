package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.User;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.repository.UserRepository;
import com.example.distribution_backernd.service.LocationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
    private final LocationLogRepository logRepo;
    private final UserRepository userRepo;
    private final LocationStreamService streamService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }

    @GetMapping("/history")
    public List<LocationLog> getHistory(
            @RequestParam Integer userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end) {
        return logRepo.findHistoryRange(userId, start, end);
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

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Driver name is required.");
        }

        User worker = new User();
        worker.setName(name);
        worker.setPhoneNumber(phoneNumber);

        userRepo.save(worker);
        return ResponseEntity.ok("Driver registered successfully with ID: " + worker.getId());
    }
}
