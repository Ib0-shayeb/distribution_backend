package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.dto.ChecklistWithItemsDTO;
import com.example.distribution_backernd.model.Checklist;
import com.example.distribution_backernd.model.ChecklistItem;
import com.example.distribution_backernd.repository.ChecklistItemRepository;
import com.example.distribution_backernd.repository.ChecklistRepository;
import com.example.distribution_backernd.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/checklists")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ManagerChecklistController {
    private final ChecklistRepository checklistRepo;
    private final ChecklistItemRepository checklistItemRepo;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> createChecklist(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String name) {

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        Checklist checklist = new Checklist(name, fleetId);
        checklistRepo.save(checklist);

        return ResponseEntity.ok("Checklist added successfully with ID: " + checklist.getId());
    }

    @GetMapping
    public List<Checklist> getAllChecklists(
            @RequestHeader("Authorization") String authHeader) {

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return checklistRepo.findByFleetId(fleetId);
    }

    @GetMapping("/driver/{driverId}")
    public List<ChecklistWithItemsDTO> getDriverChecklists(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer driverId) {

        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        List<Checklist> checklists = checklistRepo.findByFleetIdAndDriverIdWithItems(fleetId, driverId);

        return checklists.stream()
                .map(c -> new ChecklistWithItemsDTO(c, c.getItems()))
                .toList();
    }

    @GetMapping("/{checklistId}/items")
    public List<ChecklistItem> getChecklistItems(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        return checklistItemRepo.findByChecklistIdAndFleetId(checklistId, fleetId);
    }

    @PostMapping("/{checklistId}/items")
    public ResponseEntity<?> addChecklistItems(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId, @RequestBody List<ChecklistItem> items) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);
        Integer userId = jwtUtil.extractUserId(jwt);

        Checklist checklist = checklistRepo.findByIdAndFleetId(checklistId, fleetId)
                .orElseThrow(() -> new RuntimeException("Checklist with ID: " + checklistId + " does not exist"));

        for (ChecklistItem item : items) {
            item.setChecklist(checklist);
            item.setAddedById(userId);
        }

        checklistItemRepo.saveAll(items);

        return ResponseEntity.ok("Synced " + items.size() + " item records.");
    }

    @DeleteMapping("/{checklistId}/items/{itemId}")
    public ResponseEntity<?> deleteChecklistItems(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId, @PathVariable Integer itemId) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        checklistRepo.findByIdAndFleetId(checklistId, fleetId)
                .orElseThrow(() -> new RuntimeException("Checklist with ID: " + checklistId + " does not exist"));

        long deletedCount = checklistItemRepo.deleteByIdAndChecklistId(itemId, checklistId);

        if (deletedCount == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Checklist item with ID " + itemId + " was not found in checklist " + checklistId);
        }
        return ResponseEntity.ok("Removed item.");
    }

    @DeleteMapping("/{checklistId}")
    public ResponseEntity<?> deleteChecklist(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        long deletedCount = checklistRepo.deleteByIdAndFleetId(checklistId, fleetId);

        if (deletedCount == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Checklist item not found");
        }
        return ResponseEntity.ok("Removed item.");
    }

    @PutMapping("/{checklistId}/assign/{driverId}")
    public ResponseEntity<?> assignDriver(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer checklistId, @PathVariable Integer driverId) {
        String jwt = authHeader.substring(7);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        Checklist checklist = checklistRepo.findByIdAndFleetId(checklistId, fleetId)
                .orElseThrow(() -> new RuntimeException("Checklist with ID: " + checklistId + " does not exist"));

        checklist.setDriverId(driverId);
        checklistRepo.save(checklist);

        return ResponseEntity.ok("Assigned " + driverId + " to " + checklist.getId());
    }
}
