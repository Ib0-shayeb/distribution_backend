package com.example.distribution_backernd.service;

import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.Trip;
import com.example.distribution_backernd.model.User;
import com.example.distribution_backernd.repository.TripRepository;
import com.example.distribution_backernd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class LocationStreamService {
    private final UserRepository userRepository;
    private final TripRepository tripRepo;
    private final Map<Integer, List<SseEmitter>> activeStreams = new ConcurrentHashMap<>();

    public SseEmitter createStream(Integer fleetId, Integer userId) {
        // check if user belongs to fleet
        User targetUser = userRepository.findByIdAndFleetId(userId, fleetId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // set timeout to infinite and because render handles killing idle connections
        SseEmitter emitter = new SseEmitter(-1L);
        activeStreams.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((e) -> removeEmitter(userId, emitter));

        // Send initial connection event so the browser knows it worked
        try {
            emitter.send(SseEmitter.event().name("init").data("connected"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    @Scheduled(fixedRate = 60000)
    public void sendHeartbeat() {
        for (Map.Entry<Integer, List<SseEmitter>> entry : activeStreams.entrySet()) {
            Integer userId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();

            for (SseEmitter emitter : emitters) {
                try {
                    // this will make the frontend request the ../history endpoint and refresh the connection
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException e) {
                    removeEmitter(userId, emitter);
                }
            }
        }
    }

    public void broadcastLocation(LocationLog log, Integer userId) {
        List<SseEmitter> emitters = activeStreams.get(userId);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("location-update")
                            .data(""));
                            //.data(log));// the data is not being used at this point
                } catch (IOException e) {
                    removeEmitter(userId, emitter);
                }
            }
        }
    }

    private void removeEmitter(Integer userId, SseEmitter emitter) {
        List<SseEmitter> emitters = activeStreams.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                activeStreams.remove(userId);
            }
        }
    }
}