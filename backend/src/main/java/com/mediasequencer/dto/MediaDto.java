package com.mediasequencer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MediaDto(
        @JsonProperty("id") Long id,
        @JsonProperty("label") String label,
        @JsonProperty("kind") String kind,
        @JsonProperty("url") String url,
        @JsonProperty("default_duration_seconds") int defaultDurationSeconds
) {
}
