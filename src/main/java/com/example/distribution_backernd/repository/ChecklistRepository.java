package com.example.distribution_backernd.repository;

import com.example.distribution_backernd.model.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChecklistRepository extends JpaRepository<Checklist, Integer> {
    @Transactional
    long deleteByIdAndFleetId(Integer checklistId,  Integer fleetId);
    List<Checklist> findByFleetId(Integer fleetId);
    List<Checklist> findByFleetIdAndDriverId(Integer fleetId,  Integer driverId );
    Optional<Checklist> findByIdAndFleetId(Integer checklistId, Integer fleetId);
    List<Checklist> findByDriverId(Integer driverId);
    Optional<Checklist> findByIdAndDriverId(Integer checklistId, Integer fleetId);
    @Query("SELECT c FROM Checklist c LEFT JOIN FETCH c.items WHERE c.fleetId = :fleetId AND c.driverId = :driverId")
    List<Checklist> findByFleetIdAndDriverIdWithItems(
            @Param("fleetId") Integer fleetId,
            @Param("driverId") Integer driverId
    );
}
