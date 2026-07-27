package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.repository.UserRepository;
import com.example.distribution_backernd.service.LocationStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/driver/locations")
@CrossOrigin(origins = "*")
public class DriverLocationController {

    @Autowired
    private LocationLogRepository logRepo;

    @Autowired
    private LocationStreamService streamService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello!";
    }

    @PostMapping("/log")
    public LocationLog logLocation(@RequestBody LocationLog newLog) {
        newLog.setRecordedAt(ZonedDateTime.now());
        LocationLog savedLog = logRepo.save(newLog);

        streamService.broadcastLocation(newLog);

        return savedLog;
    }
}
