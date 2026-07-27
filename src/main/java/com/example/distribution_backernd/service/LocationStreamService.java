package com.example.distribution_backernd.service;

import com.example.distribution_backernd.model.LocationLog;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LocationStreamService {

    private final Map<Integer, List<SseEmitter>> activeStreams = new ConcurrentHashMap<>();

    public SseEmitter createStream(Integer userId) {
        SseEmitter emitter = new SseEmitter(1800000L);
        activeStreams.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((e) -> removeEmitter(userId, emitter));

        return emitter;
    }

    public void broadcastLocation(LocationLog log) {
        List<SseEmitter> emitters = activeStreams.get(log.getUserId());
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("location-update")
                            .data(log));
                } catch (IOException e) {
                    removeEmitter(log.getUserId(), emitter);
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