package com.example.distribution_backernd.service;

import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.model.Trip;
import com.example.distribution_backernd.repository.TripRepository;
import lombok.RequiredArgsConstructor;
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
    private final TripRepository tripRepo;
    private final Map<Integer, List<SseEmitter>> activeStreams = new ConcurrentHashMap<>();

    public SseEmitter createStream(Integer userId) {
        SseEmitter emitter = new SseEmitter(1800000L);
        activeStreams.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);

        return emitter;
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