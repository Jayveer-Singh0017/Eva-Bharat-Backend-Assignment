package com.mediasequencer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WindowItemDto(
        @JsonProperty("id") Long id,
        @JsonProperty("media_id") Long mediaId,
        @JsonProperty("position") int position,
        @JsonProperty("duration_seconds") int durationSeconds
) {
}
