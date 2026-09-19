package com.mediasequencer.controller;

import com.mediasequencer.dto.*;
import com.mediasequencer.service.SequencerService;
import com.mediasequencer.sse.SseBroker;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final SequencerService sequencerService;
    private final SseBroker sseBroker;

    public ApiController(SequencerService sequencerService, SseBroker sseBroker) {
        this.sequencerService = sequencerService;
        this.sseBroker = sseBroker;
    }

    @GetMapping("/time")
    public ResponseEntity<TimeResponse> getTime() {
        return ResponseEntity.ok(new TimeResponse(Instant.now().toString()));
    }

    @GetMapping("/state")
    public ResponseEntity<StateResponseDto> getState() {
        return ResponseEntity.ok(sequencerService.getState(Instant.now()));
    }

    @PostMapping("/windows/{id}/items")
    public ResponseEntity<WindowItemDto> addWindowItem(
            @PathVariable("id") Long id,
            @RequestBody AddItemRequest request
    ) {
        WindowItemDto item = sequencerService.addWindowItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @DeleteMapping("/windows/{id}/items/{itemId}")
    public ResponseEntity<Void> deleteWindowItem(
            @PathVariable("id") Long id,
            @PathVariable("itemId") Long itemId
    ) {
        sequencerService.deleteWindowItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/media")
    public ResponseEntity<MediaDto> createMedia(@RequestBody CreateMediaRequest request) {
        MediaDto media = sequencerService.createMedia(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(media);
    }

    @PostMapping("/sync")
    public ResponseEntity<SyncEventDto> createSync(@RequestBody CreateSyncRequest request) {
        SyncEventDto sync = sequencerService.createSync(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sync);
    }

    @PutMapping("/settings/cycle")
    public ResponseEntity<Map<String, Integer>> setCycle(@RequestBody SetCycleRequest request) {
        Map<String, Integer> result = sequencerService.setCycleSeconds(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getEvents(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        return sseBroker.subscribe();
    }
}
