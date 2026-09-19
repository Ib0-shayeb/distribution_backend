package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.dto.DriverTripHistory;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.security.JwtUtil;
import com.example.distribution_backernd.service.LocationLogOrganisation;
import com.example.distribution_backernd.service.UserStreamService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/manager/locations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ManagerLocationController {
    private final LocationLogOrganisation logOrg;
    private final LocationLogRepository logRepo;
    private final UserStreamService streamService;
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

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter streamDriverLocation(
            @RequestParam(name = "token") String token,
            @RequestParam Integer userId,
            HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        String jwt = token;
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return streamService.createStream(fleetId, userId);
    }
}
