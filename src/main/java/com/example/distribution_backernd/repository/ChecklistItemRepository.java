package com.example.distribution_backernd.repository;

import com.example.distribution_backernd.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Integer> {
    List<ChecklistItem> findByChecklistIdAndFleetId(Integer checklistId, Integer fleetId);
    void deleteByIdAndChecklistId(Integer id, Integer checklistId);
}
