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
@RequestMapping("/api/driver/checklists")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DriverChecklistController {
    private final ChecklistRepository checklistRepo;
    private final ChecklistItemRepository checklistItemRepo;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<Checklist> assignedChecklist(@RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);

        return checklistRepo.findByDriverId(userId);
    }

    @GetMapping("/with-items")
    public List<ChecklistWithItemsDTO> assignedChecklistWithItems(@RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);
        Integer fleetId = jwtUtil.extractFleetId(jwt);

        List<Checklist> checklists = checklistRepo.findByFleetIdAndDriverIdWithItems(fleetId, userId);

        return checklists.stream()
                .map(c -> new ChecklistWithItemsDTO(c, c.getItems()))
                .toList();
    }

    @GetMapping("/{checklistId}/items")
    public List<ChecklistItem> assignedChecklistItems(@RequestHeader("Authorization") String authHeader
            , @PathVariable Integer checklistId) {
        String jwt = authHeader.substring(7);
        Integer userId = jwtUtil.extractUserId(jwt);

        checklistRepo.findByIdAndDriverId(checklistId, userId)
                .orElseThrow(() -> new RuntimeException("Checklist doesent exist"));
        return checklistItemRepo.findByChecklistId(checklistId);
    }

    @PostMapping("/{checklistId}/items")
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

    @DeleteMapping("/{checklistId}/items/{itemId}")
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
