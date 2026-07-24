package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.User;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


@RestController
@RequestMapping("/api/locations")
@CrossOrigin(origins = "*")
public class LocationController {

    @Autowired
    private LocationLogRepository logRepo;

    @Autowired
    private UserRepository userRepo;

    private final Map<Integer, List<SseEmitter>> activeStreams = new ConcurrentHashMap<>();

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
        SseEmitter emitter = new SseEmitter(1800000L);

        activeStreams.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((e) -> removeEmitter(userId, emitter));

        return emitter;
    }


    @PostMapping("/log")
    public LocationLog logLocation(@RequestBody LocationLog newLog) {
        newLog.setRecordedAt(ZonedDateTime.now());
        LocationLog savedLog = logRepo.save(newLog);

        List<SseEmitter> emitters = activeStreams.get(savedLog.getUserId());
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("location-update")
                            .data(savedLog));
                } catch (IOException e) {
                    removeEmitter(savedLog.getUserId(), emitter);
                }
            }
        }

        return savedLog;
    }

    private void removeEmitter(Integer userId, SseEmitter emitter) {
        List<SseEmitter> emitters = activeStreams.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                activeStreams.remove(userId);
            }
        }
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