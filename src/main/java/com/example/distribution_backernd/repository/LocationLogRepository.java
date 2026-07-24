package com.example.distribution_backernd.repository;

import com.example.distribution_backernd.model.LocationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface LocationLogRepository extends JpaRepository<LocationLog, Integer> {

    // Returns all location records for a user from start to end (DateTime)
    @Query(value = "SELECT * FROM location_logs WHERE user_id = :userId AND recorded_at BETWEEN :start AND :end ORDER BY recorded_at ASC", nativeQuery = true)
    List<LocationLog> findHistoryRange(
            @Param("userId") Integer userId,
            @Param("start") ZonedDateTime start,
            @Param("end") ZonedDateTime end
    );

    // Pulls only user IDs active in the last 1 hour
    @Query(value = "SELECT DISTINCT user_id FROM location_logs WHERE recorded_at >= NOW() - INTERVAL '1 hour' ORDER BY user_id ASC", nativeQuery = true)
    List<Integer> findActiveUserIds();
}