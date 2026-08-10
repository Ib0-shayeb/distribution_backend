package com.example.distribution_backernd.repository;

import com.example.distribution_backernd.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Integer> {
    @Query("SELECT ci FROM ChecklistItem ci JOIN Checklist c ON ci.checklistId = c.id WHERE c.id = :checklistId AND c.fleetId = :fleetId")
    List<ChecklistItem> findByChecklistIdAndFleetId(@Param("checklistId") Integer checklistId, @Param("fleetId") Integer fleetId);
    void deleteByIdAndChecklistId(Integer id, Integer checklistId);
}
