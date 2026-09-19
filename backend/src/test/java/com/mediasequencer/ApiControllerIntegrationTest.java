package com.mediasequencer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediasequencer.dto.AddItemRequest;
import com.mediasequencer.dto.CreateMediaRequest;
import com.mediasequencer.dto.CreateSyncRequest;
import com.mediasequencer.dto.SetCycleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /health returns ok")
    void testHealth() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("GET /api/time returns server_time")
    void testTime() throws Exception {
        mockMvc.perform(get("/api/time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server_time").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/state returns seeded state with windows and media")
    void testGetState() throws Exception {
        mockMvc.perform(get("/api/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.server_time").isNotEmpty())
                .andExpect(jsonPath("$.anchor").value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.cycle_seconds").value(18000))
                .andExpect(jsonPath("$.media", hasSize(6)))
                .andExpect(jsonPath("$.windows", hasSize(4)))
                .andExpect(jsonPath("$.windows[0].name").value("Window 1"))
                .andExpect(jsonPath("$.windows[0].items", hasSize(3)))
                .andExpect(jsonPath("$.windows[0].resolved.remaining_seconds", greaterThan(0.0)));
    }

    @Test
    @DisplayName("POST /api/windows/{id}/items adds item to window")
    void testAddWindowItem() throws Exception {
        AddItemRequest req = new AddItemRequest(1L, 12);
        mockMvc.perform(post("/api/windows/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.media_id").value(1))
                .andExpect(jsonPath("$.position").value(4)) // Window 1 had 3 items
                .andExpect(jsonPath("$.duration_seconds").value(12));
    }

    @Test
    @DisplayName("POST /api/windows/{id}/items validation and error handling")
    void testAddWindowItemErrors() throws Exception {
        // Non-existent window
        mockMvc.perform(post("/api/windows/999/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddItemRequest(1L, 10))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("window 999: not found"));

        // Non-existent media
        mockMvc.perform(post("/api/windows/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddItemRequest(999L, 10))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("media 999: not found"));

        // Missing media_id
        mockMvc.perform(post("/api/windows/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration_seconds\": 10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("media_id is required"));

        // Invalid duration
        mockMvc.perform(post("/api/windows/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddItemRequest(1L, -5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("duration_seconds must be positive"));

        // Non-numeric window id in path
        mockMvc.perform(post("/api/windows/abc/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddItemRequest(1L, 10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid window id"));
    }

    @Test
    @DisplayName("DELETE /api/windows/{id}/items/{itemId} removes item")
    void testDeleteWindowItem() throws Exception {
        mockMvc.perform(delete("/api/windows/1/items/1"))
                .andExpect(status().isNoContent());

        // Second delete fails with 404
        mockMvc.perform(delete("/api/windows/1/items/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("window item 1 in window 1: not found"));
    }

    @Test
    @DisplayName("POST /api/media creates media and validates fields")
    void testCreateMedia() throws Exception {
        CreateMediaRequest req = new CreateMediaRequest("New Media", "image", "/media/new.png", 20);
        mockMvc.perform(post("/api/media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.label").value("New Media"))
                .andExpect(jsonPath("$.kind").value("image"))
                .andExpect(jsonPath("$.url").value("/media/new.png"))
                .andExpect(jsonPath("$.default_duration_seconds").value(20));

        // Missing label
        mockMvc.perform(post("/api/media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMediaRequest("", "image", "/url", 10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("label is required"));

        // Invalid kind
        mockMvc.perform(post("/api/media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMediaRequest("M", "audio", "/url", 10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("kind must be one of image, video, blank"));

        // Empty URL for non-blank kind
        mockMvc.perform(post("/api/media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMediaRequest("M", "image", "", 10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("url is required unless kind is blank"));

        // Non-positive duration
        mockMvc.perform(post("/api/media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateMediaRequest("M", "image", "/url", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("default_duration_seconds must be positive"));
    }

    @Test
    @DisplayName("POST /api/sync creates active sync overlay")
    void testCreateSync() throws Exception {
        CreateSyncRequest req = new CreateSyncRequest(1L, 30);
        mockMvc.perform(post("/api/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.media_id").value(1))
                .andExpect(jsonPath("$.duration_seconds").value(30))
                .andExpect(jsonPath("$.start_at").isNotEmpty());

        // Verify active sync appears in state
        mockMvc.perform(get("/api/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active_sync.media_id").value(1))
                .andExpect(jsonPath("$.active_sync.duration_seconds").value(30))
                .andExpect(jsonPath("$.windows[0].resolved.is_sync").value(true))
                .andExpect(jsonPath("$.windows[0].resolved.media_id").value(1));
    }

    @Test
    @DisplayName("PUT /api/settings/cycle updates cycle seconds")
    void testSetCycle() throws Exception {
        SetCycleRequest req = new SetCycleRequest(60);
        mockMvc.perform(put("/api/settings/cycle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycle_seconds").value(60));

        // State reflects updated cycle
        mockMvc.perform(get("/api/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycle_seconds").value(60));

        // Out of bounds (< 10)
        mockMvc.perform(put("/api/settings/cycle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SetCycleRequest(5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("cycle_seconds must be between 10 and 86400"));

        // Out of bounds (> 86400)
        mockMvc.perform(put("/api/settings/cycle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SetCycleRequest(100000))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("cycle_seconds must be between 10 and 86400"));
    }

    @Test
    @DisplayName("CORS headers are set correctly")
    void testCors() throws Exception {
        mockMvc.perform(get("/health").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", "Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"));

        mockMvc.perform(options("/api/state").header("Origin", "http://localhost:5173"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
