package com.example.distribution_backernd.service;

import com.example.distribution_backernd.dto.DriverTripHistory;
import com.example.distribution_backernd.dto.FlatTripLogProjection;
import com.example.distribution_backernd.dto.LocationLogDTO;
import com.example.distribution_backernd.dto.TripHistory;
import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.repository.LocationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationLogOrganisation {

    private final LocationLogRepository logRepo;

//    public List<TripHistory> getDriverTripHistory(Integer userId, ZonedDateTime start, ZonedDateTime end) {
//        List<LocationLog> flatLogs = logRepo.findHistoryRange(userId, start, end);
//
//        Map<Integer, List<LocationLogDTO>> logsByTripMap = flatLogs.stream()
//                .collect(Collectors.groupingBy(
//                        LocationLog::getTripId,
//                        LinkedHashMap::new,
//                        Collectors.mapping(
//                                log -> new LocationLogDTO(
//                                        log.getLatitude(),
//                                        log.getLongitude()
//                                ),
//                                Collectors.toList()
//                        )
//                ));
//
//        // Convert map entries to TripHistoryDTO list
//        return logsByTripMap.entrySet().stream()
//                .map(entry -> new TripHistory(
//                        entry.getKey(),
//                        entry.getValue()
//                ))
//                .toList();
//    }

    public List<DriverTripHistory> getBatchDriverTripHistory(Integer fleetId, List<Integer> userIds, ZonedDateTime start, ZonedDateTime end) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<FlatTripLogProjection> flatRows = logRepo.findHistoryRangeForUsers(fleetId, userIds, start, end);

        Map<Integer, Map<Integer, List<LocationLogDTO>>> grouped = flatRows.stream()
                .collect(Collectors.groupingBy(
                        FlatTripLogProjection::getUserId,
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                FlatTripLogProjection::getTripId,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        row -> new LocationLogDTO(row.getLatitude(), row.getLongitude()),
                                        Collectors.toList()
                                )
                        )
                ));

        return grouped.entrySet().stream()
                .map(userEntry -> new DriverTripHistory(
                        userEntry.getKey(),
                        userEntry.getValue().entrySet().stream()
                                .map(tripEntry -> new TripHistory(
                                        tripEntry.getKey(),
                                        tripEntry.getValue()
                                )).toList()
                )).toList();
    }
}