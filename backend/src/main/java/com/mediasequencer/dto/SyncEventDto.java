package com.mediasequencer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SyncEventDto(
        @JsonProperty("media_id") Long mediaId,
        @JsonProperty("start_at") String startAt,
        @JsonProperty("duration_seconds") int durationSeconds
) {
}
