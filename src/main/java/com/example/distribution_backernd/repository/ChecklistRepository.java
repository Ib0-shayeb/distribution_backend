package com.example.distribution_backernd.repository;

import com.example.distribution_backernd.model.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChecklistRepository extends JpaRepository<Checklist, Integer> {
    List<Checklist> findByFleetId(Integer fleetId);
    Optional<Checklist> findByIdAndFleetId(Integer checklistId, Integer fleetId);
    List<Checklist> findByDriverId(Integer driverId);
    Optional<Checklist> findByIdAndDriverId(Integer checklistId, Integer fleetId);

}
