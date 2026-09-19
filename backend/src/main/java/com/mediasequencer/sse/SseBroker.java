package com.mediasequencer.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pub/Sub SSE broker matching Go's buffered, non-blocking fan-out broker.
 */
@Component
public class SseBroker {

    private static final Logger log = LoggerFactory.getLogger(SseBroker.class);

    private final CopyOnWriteArrayList<SseEmitter> subscribers = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // Infinite timeout for SSE stream
        SseEmitter emitter = new SseEmitter(0L);

        subscribers.add(emitter);

        emitter.onCompletion(() -> subscribers.remove(emitter));
        emitter.onTimeout(() -> {
            subscribers.remove(emitter);
            emitter.complete();
        });
        emitter.onError(e -> {
            subscribers.remove(emitter);
            emitter.complete();
        });

        // Confirm stream is live with initial handshake event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{}"));
        } catch (IOException e) {
            subscribers.remove(emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Broadcasts event to all active subscribers non-blockingly.
     */
    public void broadcast(String event) {
        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event)
                        .data("{}"));
            } catch (Exception e) {
                subscribers.remove(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Sends a comment line heartbeat every 20 seconds to prevent reverse proxy idle timeouts.
     * Generates `: ping\n\n`
     */
    @Scheduled(fixedRate = 20000)
    public void sendHeartbeat() {
        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                subscribers.remove(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public int getSubscriberCount() {
        return subscribers.size();
    }
}
