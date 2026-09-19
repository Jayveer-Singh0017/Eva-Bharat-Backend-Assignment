package com.mediasequencer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AddItemRequest(
        @JsonProperty("media_id") Long mediaId,
        @JsonProperty("duration_seconds") Integer durationSeconds
) {
}
