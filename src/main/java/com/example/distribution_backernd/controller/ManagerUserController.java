package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.model.Authority;
import com.example.distribution_backernd.model.User;
import com.example.distribution_backernd.repository.AuthorityRepository;
import com.example.distribution_backernd.repository.UserRepository;
import com.example.distribution_backernd.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ManagerUserController {
    private final UserRepository userRepo;
    private final AuthorityRepository authorityRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<User> getAllWorkers(
            @RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return userRepo.findByFleetId(fleetId);
    }

    @PostMapping("/register")
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
}
