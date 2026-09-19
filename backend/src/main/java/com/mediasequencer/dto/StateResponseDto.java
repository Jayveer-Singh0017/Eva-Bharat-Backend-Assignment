package com.mediasequencer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StateResponseDto(
        @JsonProperty("server_time") String serverTime,
        @JsonProperty("anchor") String anchor,
        @JsonProperty("cycle_seconds") int cycleSeconds,
        @JsonProperty("media") List<MediaDto> media,
        @JsonProperty("windows") List<WindowDto> windows,
        @JsonProperty("active_sync") SyncEventDto activeSync
) {
}
