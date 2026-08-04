package com.example.distribution_backernd.repository;

import com.example.distribution_backernd.dto.FlatTripLogProjection;
import com.example.distribution_backernd.model.LocationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface LocationLogRepository extends JpaRepository<LocationLog, Integer> {

    @Query(value = """
        SELECT l.* 
        FROM location_logs l
        JOIN trips t ON l.trip_id = t.id
        WHERE t.user_id = :userId 
          AND l.recorded_at BETWEEN :start AND :end 
        ORDER BY l.trip_id ASC, l.recorded_at ASC
        """, nativeQuery = true)
    List<LocationLog> findHistoryRange(
            @Param("userId") Integer userId,
            @Param("start") ZonedDateTime start,
            @Param("end") ZonedDateTime end
    );

    @Query(value = """
        SELECT DISTINCT t.user_id
        FROM location_logs l
        JOIN trips t ON l.trip_id = t.id
        WHERE l.recorded_at >= NOW() - INTERVAL '1 hour'
        ORDER BY t.user_id ASC
        """, nativeQuery = true)
    List<Integer> findActiveUserIds();

    @Query(value = """
    SELECT 
        t.user_id AS userId, 
        l.trip_id AS tripId, 
        l.latitude AS latitude, 
        l.longitude AS longitude
    FROM location_logs l
    JOIN trips t ON l.trip_id = t.id
    WHERE t.user_id IN (:userIds) 
      AND l.recorded_at BETWEEN :start AND :end 
    ORDER BY t.user_id ASC, l.trip_id ASC, l.recorded_at ASC
    """, nativeQuery = true)
    List<FlatTripLogProjection> findHistoryRangeForUsers(
            @Param("userIds") List<Integer> userIds,
            @Param("start") ZonedDateTime start,
            @Param("end") ZonedDateTime end
    );
}